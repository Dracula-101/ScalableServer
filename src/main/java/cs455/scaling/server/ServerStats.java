package cs455.scaling.server;

import cs455.scaling.helpers.Constants;

import java.util.Timer;
import java.util.TimerTask;


public class ServerStats extends TimerTask {
    private Timer timer;
    private Server server;

    ServerStats(Server server) {
        this.server = server;
        this.timer = new Timer("ServerStatsDisplayer");
    }

    @Override
    public void run() {
        server.printStats(); //Invoke print statistics method in Server class
    }

    void startExecution() {
        timer.scheduleAtFixedRate(this, Constants.STATS_LOGGER_START_DELAY_MILLIS, Constants.STATS_LOGGER_INTERVAL_MILLIS);
    }
}
