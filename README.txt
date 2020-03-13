For Graders:

Once the tar file has been extracted, inside the root directory I used the command "gradlew clean build" to build the project. The built jar can be found in the folder build/libs/ and it will be called scaling-1.0-SNAPSHOT.jar
This jar can now be used for all execution/grading purposes.

For the Client implementation, my program will take 3 arguments, server-hostname(type:String), server-port-number(type:int), message-rate(type:int) {In that order}
For the Server implementation, my program will take 4 arguments, port-number(type:int), thread-pool-size(type:int), batch-size(type:int), batch-time(type:double) {In that order}


Package Descriptions:

1) cs455.scaling.client
This package contains the implementations of the Client and its various directly linked helper classes.


	Class Descriptions in package cs455.scaling.client:

	1a) cs455.scaling.client.Client
	
	The Client class encapsulates all functionalities of the Client and coordinates message sending at a fixed rate to the server. The Client also listens for responses from the server. The Client 	 matches the hashcode sent by the server and keeps track of messages received. The client accomplishes this by opening a socket channel to the server and writing and reading communications over 	 this channel. Every 20 seconds the client prints out statistics for the number of messages exchanged between this Client and the lone server.

	1b) cs455.scaling.client.ClientStats
	
	This class is a Timer Task that utilises a timer and subscribes itself to it. This class utilises an interval of Constants.STATS_LOGGER_INTERVAL_MILLIS(which is set to 20 seconds) in order to 	schedule the printing of Client Statistics at regular intervals.

	1c) cs455.scaling.client.SendMessageAsync

	This class enables each client to aynchronously send randomly generated payloads of 8KB each at regular intervals ( (1000/message-rate) is the configured delay ) to the lone server.


2) cs455.scaling.helpers
This package contains a couple of helper classes particular to this implementation. 


	Class Descriptions in package cs455.scaling.helpers:

	2a) cs455.scaling.helpers.Constants
	
	This is a class encapsulating implementation-level constants such as debug mode and time intervals for statistics etcetera.

	2b) cs455.scaling.helpers.Hasher

	This is a class containing a singualr static method that returns the SHA-1 hash for a given byte array.


3) cs455.scaling.server
This package contains the implementations of the Server and its various directly linked helper classes.

	
	Class Descriptions in package cs455.scaling.server:

	3a) cs455.scaling.server.Server

	The Server class encapsulates all functionalities of the Server and coordinates all tasks by designating them to the thread pool. The Server listens for messages from the various clients that have 		opened connection with the server. The Server has many tasks, namely, accepting client connections, listening for client messages in a non-blocking fashion, computing hashcode of message sent by 		client and reporting the hash back to client and the batching of these tasks as well. Every 20 seconds the Server will also print statistics involving its throughput and per-client efficiencies 		and deviations.

	3b) cs455.scaling.server.ServerStats

	This class is a Timer Task that utilises a timer and subscribes itself to it. This class utilises an interval of Constants.STATS_LOGGER_INTERVAL_MILLIS(which is set to 20 seconds) in order to 	schedule the printing of Server Statistics at regular intervals.

	3c) cs455.scaling.server.TaskExecutorThread

	This class is the implementation of a worker thread that waits when the queue is empty and as soon as it gets a notify signal, it polls a task from the blocking queue, executes it and goes back to 		waiting.

	3d) cs455.scaling.server.ThreadPool
	
	Class managing the thread pool. This class is responsible for storing the task list, for notifying executor threads about new tasks that are ready to be executed. This class is responsible for the 		batching of tasks and for initializing the executor(worker) threads.


4) cs455.scaling.tasks
This package contains abstractions for the various tasks that are queued by the server and executed by the executor threads (worker threads).


	Class Descriptions in package cs455.scaling.tasks

	4a) cs455.scaling.tasks.BatchExecutorTask

	This is a class implementing the TaskInterface interface in order to do a task, which in this case is to execute all tasks in a batch one by one. This is one of the tasks executed by any one of 		the threads in the thread pool.
	
	4b) cs455.scaling.tasks.AcceptConnectionsTask

	This is a class implementing the TaskInterface interface in order to do a task, which in this case is to invoke the acceptConnections method in the Server which enables the server to accept 		incoming client connections. This is one of the tasks executed by any one of the threads in the thread pool.

	4c) cs455.scaling.tasks.DoReadWriteTask

	This is a class implementing the TaskInterface interface in order to do a task, which in this case is to read a message from the Client and to compute the corresponding hashcode and relay the 	hashcode back to the Client. This is one of the tasks executed by any one of the threads in the thread pool.

	4d) cs455.scaling.tasks.TaskInterface

	An abstraction of the task that needs to be performed when each of the actual implementations are invoked.




