import java.io.*;
import java.net.Socket;

public class NodeClientThread extends Thread 
{
    final ObjectInputStream in;
    final Socket s;
    final int id, serverId, serverIndex;
    final FileRecord[] votes;
  
    public NodeClientThread(Socket s, int id, int serverId, FileRecord[] votes) throws IOException
    {
        this.s = s;
        this.in = new ObjectInputStream(s.getInputStream());
        this.id = id;
        this.serverId = serverId;
        if (serverId >= id)
            serverIndex = serverId - 1;
        else
            serverIndex = serverId;
        this.votes = votes;
    }
  
    @Override
    public void run() 
    {
        System.out.printf("client connected to server %d\n", serverId);

        while (true) {
            try {
                // receive message
                FileRecord record = (FileRecord) in.readObject();
                System.out.printf("client received from server %d: %s\n", serverId, record.toString());
                synchronized (votes) {
                    votes[serverIndex - 1] = record;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        } 
    }
}