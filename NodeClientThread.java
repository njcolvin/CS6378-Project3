import java.io.*;
import java.net.Socket;

public class NodeClientThread extends Thread 
{
    final ObjectInputStream in;
    final Socket s;
    final int id, serverId, serverIndex;
    final FileRecord[] votes;
  
    public NodeClientThread(Socket s, int id, int serverId, int serverIndex, FileRecord[] votes) throws IOException
    {
        this.s = s;
        this.in = new ObjectInputStream(s.getInputStream());
        this.id = id;
        this.serverId = serverId;
        this.serverIndex = serverIndex;
        this.votes = votes;
    }
  
    @Override
    public void run() 
    {
        System.out.printf("client connected to server %d\n", serverId);

        int currentRound = 0;
        try {
            while (currentRound < 2) {
                // receive message
                FileRecord record = (FileRecord) in.readObject();
                System.out.printf("client received from server %d: %s\n", serverId, record.toString());
                synchronized (votes) {
                    votes[serverIndex] = record;
                }
                currentRound++;
            }
            this.s.close();
            System.out.printf("client disconnected from server %d\n", serverId);
        } catch (EOFException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}