import java.io.*;
import java.net.Socket;

public class NodeClientThread extends Thread 
{
    final ObjectInputStream in;
    final Socket s;
    final int id, serverId;
  
    // Constructor
    public NodeClientThread(Socket s, int id, int serverId) throws IOException
    {
        this.s = s;
        this.in = new ObjectInputStream(s.getInputStream());
        this.id = id;
        this.serverId = serverId;
    }
  
    @Override
    public void run() 
    {
        while (true) {
            try {
            
                System.out.printf("client connected to server %d\n", serverId);
                // receive message
                Message current_message = (Message) in.readObject();

                if (current_message instanceof MessageRequest) {
                    MessageRequest req = (MessageRequest) current_message;
                } else {
                    MessageRelease rel = (MessageRelease) current_message;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        } 
    }
}