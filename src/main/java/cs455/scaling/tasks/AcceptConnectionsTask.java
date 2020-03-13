package cs455.scaling.tasks;

import cs455.scaling.server.Server;

import java.nio.channels.SelectionKey;

/**
 * Class implementing the TaskInterface interface in order to do a task, which in this case is to invoke the acceptConnections
 * method in the Server. This is one of the tasks executed by any one of the threads in the thread pool.
 */
public class AcceptConnectionsTask implements TaskInterface {
    private Server server;
    private SelectionKey selectionKey;

    public AcceptConnectionsTask(Server server, SelectionKey selectionKey) {
        this.server = server;
        this.selectionKey = selectionKey;
    }

    @Override
    public void onTask() {
        server.acceptConnections(selectionKey);
    }
}
