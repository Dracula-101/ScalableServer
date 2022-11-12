package cs455.scaling.server;

import cs455.scaling.helpers.Constants;
import cs455.scaling.tasks.AcceptConnectionsTask;
import cs455.scaling.tasks.DoReadWriteTask;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;


public class Server {
    private Selector selector;
    private volatile AtomicInteger tasksServedByServer;
    private HashMap<SocketChannel, Integer> perClientStatsMap;
    private ServerSocketChannel serverSocketChannel;
    private ThreadPool threadPool;

    private Server(int port, int poolSize, int batchSize, double batchTime) throws IOException {
        tasksServedByServer = new AtomicInteger(0);
        perClientStatsMap = new HashMap<>();

        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false); //enable non-blocking I/O
        serverSocketChannel.socket().bind(new InetSocketAddress(port)); //bind serversocket to the designated port
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT); //register intent to accept connections
        System.out.println("Server listening on port " + port);

        new ServerStats(this).startExecution();
        threadPool = new ThreadPool(batchSize, batchTime * 1000, poolSize); //Initialize the thread pool class with user-defined constraints
        threadPool.initiateThreads(); //Start each thread the thread pool and make them subscribe to the task queue
        startKeyWiseMultiplexing();
    }

    private void startKeyWiseMultiplexing() {
        while (true) {
            try {
                selector.selectNow();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Set<SelectionKey> selectedKeys = selector.selectedKeys(); //Some channel has activity, get the keys
            Iterator<SelectionKey> it = selectedKeys.iterator(); //Get an iterator over the keys to make life easier

            while (it.hasNext()) {
                SelectionKey selectionKey = it.next();
                it.remove();
                if (selectionKey.isAcceptable()) {
                    selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_ACCEPT);
                    AcceptConnectionsTask task = new AcceptConnectionsTask(this, selectionKey); //create an accept connections task
                    //threadPool.addTaskToBatch(task);
                    threadPool.notifyAndExecuteImmediate(task); // Add accept connections task to immediate list and notify available threads of this slightly prioritised task
                    if (Constants.DEBUG) {
                        System.out.println("Adding Accept Task to tasklist in Server");
                    }
                } else if (selectionKey.isReadable()) {
                    SocketChannel clientChannel = (SocketChannel) selectionKey.channel();
                    tasksServedByServer.incrementAndGet(); //Increment server tasks served

                    if (Constants.DEBUG) {
                        System.out.println("Adding Read/Write Task to tasklist in Server");
                    }

                    synchronized (perClientStatsMap) {
                        int count = perClientStatsMap.get(clientChannel);
                        perClientStatsMap.put(clientChannel, ++count); //Increment message received count for particular client(indexed by their channel)
                    }

                    DoReadWriteTask readWriteTask = new DoReadWriteTask(selectionKey); //Create read-write task
                    selectionKey.interestOps(selectionKey.interestOps() & SelectionKey.OP_READ);
                    threadPool.addTaskToBatch(readWriteTask); //Add read-write task to task list to be batched
                }
            }
        }
    }

    public void acceptConnections(SelectionKey key) {
        try {
            if (Constants.DEBUG) {
                System.out.println("Adding new connection in Server");
            }
            SocketChannel clientChannel = serverSocketChannel.accept();
            clientChannel.configureBlocking(false); //enable non-blocking I/O on channel
            clientChannel.register(selector, SelectionKey.OP_READ);

            synchronized (perClientStatsMap) {
                perClientStatsMap.put(clientChannel, 0); //Add entry for this client in the client message count map.
            }

            key.interestOps(key.interestOps() | SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    synchronized void printStats() {
        synchronized (perClientStatsMap) {
            System.out.println("--------------------------------------------------------");
            System.out.println("\n(" + System.currentTimeMillis() + ")");
            System.out.println(
                    "Server Throughtput\t\t" + (tasksServedByServer.get()) / Constants.STATS_LOGGER_INTERVAL_SECS);
            System.out.println("Active Client Connections\t" + perClientStatsMap.size());
            System.out.println("Mean Per-Client Throughput\t"
                    + (meanPerClientThroughput()) / Constants.STATS_LOGGER_INTERVAL_SECS + " messages");
            tasksServedByServer.set(0);
        }
    }

    private double meanPerClientThroughput() {
        int sum = 0;
        for (SocketChannel channel : perClientStatsMap.keySet()) {
            sum += perClientStatsMap.get(channel);
        }
        return (double) sum / perClientStatsMap.size();
    }



    public static void main(String[] args) {
        try {
            new Server(5000, 12, 10, 2.5);
        } catch (IOException e) {
            if (Constants.DEBUG) {
                System.out.println("Server Constructor threw error");
            }
            e.printStackTrace();
        }
    }
}
