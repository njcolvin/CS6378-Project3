import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

class NodeServerThread extends Thread {
    ObjectOutputStream out;
    ServerSocket server;
    Socket s;
    int clientId;
    final int id;
    AtomicInteger votingRound;
    AtomicReference<FileRecord> record;

    public NodeServerThread(ServerSocket server,
                            int id,
                            AtomicInteger votingRound,
                            AtomicReference<FileRecord> record) throws IOException
    {
        this.server = server;
        this.id = id;
        this.votingRound = votingRound;
        this.record = record;
    }

    @Override
    public void run() {

        try {
            this.s = server.accept();
            this.out = new ObjectOutputStream(s.getOutputStream());
            this.out.flush();
            String currentIp = s.getInetAddress().toString();
            this.clientId = Integer.parseInt(currentIp.substring(currentIp.length() - 2)) - 31;
            System.out.printf("server connected to client %d\n", clientId);

            int currentRound = 0;
            while (currentRound < 8) {
                
                if (currentRound == votingRound.get()) {
                    this.out.writeObject(record.get());
                    this.out.flush();
                    currentRound++;
                }

                Thread.sleep(100);
                
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
}