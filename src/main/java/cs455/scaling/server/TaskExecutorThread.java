package cs455.scaling.server;

import cs455.scaling.tasks.TaskInterface;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class is the implementation of a worker thread that waits when the queue is empty and as soon as it gets a notify
 * signal, it polls a task from the blocking queue, executes it and goes back to waiting.
 */
public class TaskExecutorThread extends Thread {
    private LinkedBlockingQueue<TaskInterface> FIFO_QUEUE; //Thread-safe blocking queue for queueing tasks from which threads can pick a task and execute

    TaskExecutorThread(LinkedBlockingQueue<TaskInterface> FIFO_QUEUE) {
        this.FIFO_QUEUE = FIFO_QUEUE;
    }

    public void run() {
        while (true) {
            synchronized (FIFO_QUEUE) {
                while (FIFO_QUEUE.isEmpty()) {
                    try {
                        FIFO_QUEUE.wait(); //Send thread to waiting state
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                TaskInterface task = FIFO_QUEUE.poll(); //Poll task from blocking queue
                if (task != null) {
                    task.onTask(); //Execute task
                }
            }
        }
    }
}
