package cs455.scaling.tasks;

import java.util.LinkedList;

/**
 * Class implementing the TaskInterface interface in order to do a task, which in this case is to execute all tasks in
 * a batch one by one. This is one of the tasks executed by any one of the threads in the thread pool.
 */
public class BatchExecutorTask implements TaskInterface {
    private LinkedList<TaskInterface> batch;

    public BatchExecutorTask(LinkedList<TaskInterface> batch) {
        this.batch = batch;
    }

    @Override
    public void onTask() {
        for (TaskInterface taskInterface : batch) {
            taskInterface.onTask(); //Execute the task for eaach instance of task
        }
    }
}
