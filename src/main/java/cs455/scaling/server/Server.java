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

/**
 * The Server class encapsulates all functionalities of the Server and coordinates all tasks by designating them to the thread pool.
 * The Server listens for messages from the various clients that have opened connection with the server. The Server has many tasks,
 * namely, accepting client connections, listening for client messages in a non-blocking fashion, computing hashcode of message
 * sent by client and reporting the hash back to client and the batching of these tasks as well. Every 20 seconds the Server will
 * also print statistics involving its throughput and per-client efficiencies and deviations.
 */

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

        new ServerStats(this).startExecution();
        threadPool = new ThreadPool(batchSize, batchTime * 1000, poolSize); //Initialize the thread pool class with user-defined constraints
        threadPool.initiateThreads(); //Start each thread the thread pool and make them subscribe to the task queue
        startKeyWiseMultiplexing();
    }

    /**
     * This function uses the the non-blocking selectNow call to get the set of keys from an active channel and uses
     * these keys to either accept connections on the channel or to facilitate reading of messages from the clients
     * over the channel.
     */
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

    /**
     * Function to establish connection to a client and to initialize per-client statistics for this client using the
     * selection key that was passed from the task.
     * @param key
     */
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
            System.out.println("\n[" + System.currentTimeMillis() + "]\nServer Throughput: " + (tasksServedByServer.get() / Constants.STATS_LOGGER_INTERVAL_SECS)
                    + " messages/s\nActive Client Connections: " + perClientStatsMap.size() + "\nMean Per-Client Throughput: "
                    + (meanPerClientThroughput() / Constants.STATS_LOGGER_INTERVAL_SECS) + " messages/s\nStd. Dev. of Per-Client Throughput: "
                    + (stdDevPerClientThroughput(meanPerClientThroughput()) / Constants.STATS_LOGGER_INTERVAL_SECS) + " messages/s\n");
            tasksServedByServer.set(0);
        }
    }

    /**
     * Calculate mean throughput per-client
     * @return
     */
    private double meanPerClientThroughput() {
        int sum = 0;
        for (SocketChannel channel : perClientStatsMap.keySet()) {
            sum += perClientStatsMap.get(channel);
        }
        return (double) sum / perClientStatsMap.size();
    }

    /**
     * Use the mean to calculate per-client standard deviation per second
     * @param mean
     * @return
     */
    private double stdDevPerClientThroughput(double mean) {
        double sd = 0.0;

        for (SocketChannel channel : perClientStatsMap.keySet()) {
            sd += Math.pow(perClientStatsMap.get(channel) - mean, 2);
            perClientStatsMap.put(channel, 0);
        }

        return Math.sqrt(sd / perClientStatsMap.size()) / Constants.STATS_LOGGER_INTERVAL_SECS;
    }


    public static void main(String[] args) {
        // if (args.length < 4) {
        // System.out.println("Please provide 4 arguments.\nUsage: "
        // + "java cs455.scaling.server.Server <portNum> <thread-pool-size> <batch-size>
        // <batch-time>\n"
        // + "Exiting");
        // System.exit(1);
        // }
        try {
            new Server(7000, 12, 10, 2.5);
        } catch (IOException e) {
            if (Constants.DEBUG) {
                System.out.println("Server Constructor threw error");
            }
            e.printStackTrace();
        }
    }
}
