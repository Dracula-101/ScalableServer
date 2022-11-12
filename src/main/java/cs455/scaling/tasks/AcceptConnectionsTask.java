package cs455.scaling.tasks;

import cs455.scaling.server.Server;

import java.nio.channels.SelectionKey;


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
