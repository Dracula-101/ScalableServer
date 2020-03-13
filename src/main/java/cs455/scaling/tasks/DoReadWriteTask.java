package cs455.scaling.tasks;

import cs455.scaling.helpers.Constants;
import cs455.scaling.helpers.Hasher;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Class implementing the TaskInterface interface in order to do a task, which in this case is to read a message from
 *the Client and to compute the corresponding hashcode and relay the hashcode back to the Client. This is one of the tasks
 * executed by any one of the threads in the thread pool.
 */
public class DoReadWriteTask implements TaskInterface {
    private SelectionKey selectionKey;

    public DoReadWriteTask(SelectionKey selectionKey) {
        this.selectionKey = selectionKey;
    }

    @Override
    public void onTask() {
        SocketChannel channel = (SocketChannel) selectionKey.channel(); //Extract channel from selection key
        ByteBuffer readBuffer = ByteBuffer.allocate(Constants.BYTES_PER_MESSAGE);

        try {
            int numBytesRead = 0;
            while (readBuffer.hasRemaining() && numBytesRead != -1) {
                numBytesRead = channel.read(readBuffer); //Read bytes into a byte buffer
            }
            selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_READ);

        } catch (IOException e) {
            e.printStackTrace();
        }
        String hashOfPayload = Hasher.SHA1FromBytes(readBuffer.array());
        hashOfPayload = String.format("%40s", hashOfPayload).replace(" ", "-"); //Pad hash to make it length 40. Essential for synchronizing sizes of messages
        byte[] hashBytes = hashOfPayload.getBytes();
        ByteBuffer byteBuffer = ByteBuffer.wrap(hashBytes); //Wrap computed hash into a byte buffer
        while (byteBuffer.hasRemaining()) {
            try {
                if (Constants.DEBUG) {
                    System.out.println("Writing hash to Client " + hashOfPayload);
                }
                channel.write(byteBuffer); //Write hash to the client that sent the message
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
