import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Node {
    private int id;
    private final int[][][] partitions = {
        {{1, 2, 3, 4, 5, 6, 7, 8}}, {{1, 2, 3, 4}, {5, 6, 7, 8}}, 
        {{1}, {2, 3, 4}, {5, 6, 7}, {8}}, {{1}, {2, 3, 4, 5, 6, 7}, {8}}
    };
    private final String ip = "10.176.69.";
    private final int startIp = 32;
    private final int numNodes = 8;
    private final NodeClientThread[] clientThreads;
    private final NodeServerThread[] serverThreads;
    private AtomicReference<FileRecord> record;
    private AtomicInteger votingRound;
    private FileRecord[] votes;

    public Node(int id) {
        this.id = id;
        this.clientThreads = new NodeClientThread[numNodes - 1];
        this.serverThreads = new NodeServerThread[numNodes - 1];
        this.record = new AtomicReference<FileRecord>();
        record.set(new FileRecord(1, 8, 1));
        this.votingRound = new AtomicInteger(-1);
        this.votes = new FileRecord[numNodes - 1];
        startServers();
        startClients();
        run();

    }

    // TODO: partition network helpers
    //  private void startServer(int id);
    //  private void stopServer(int id);
    //  private void startClient(int id);
    //  private void stopClient(int id);

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startServers() {
    
        System.out.printf("node %d starting servers\n", id);
        
        try {
            ServerSocket serverSocket = new ServerSocket(5056);
            serverSocket.setSoTimeout(0);
            for (int i = 0; i < serverThreads.length; i++) {
                serverThreads[i] = new NodeServerThread(serverSocket, id, votingRound, record);
                serverThreads[i].start();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startClients() {

        System.out.printf("node %d starting clients\n", id);
        int i = 0;
        for (int ip_suffix = startIp; ip_suffix < startIp + numNodes; ip_suffix++) {
            if (i == id - 1){
                i++;
                continue;
            }
            // try to connect to a server once per second
            while (true) {
                if (!connectToClient(i)) {
                    sleep(1000);
                }
                else {
                    i++;
                    break;
                }
            }
        }

    }

    private boolean connectToClient(int serverId) {
        int serverIndex = serverId;
        if (serverIndex >= id)
            serverIndex--;

        try {
            System.out.println("connecting to " + ip + String.valueOf(startIp + serverId));
            Socket s = new Socket(ip + String.valueOf(startIp + serverId), 5056);
            clientThreads[serverIndex] = new NodeClientThread(s, id, serverId + 1, votes);
            clientThreads[serverIndex].start();
            return true;
        }
        catch (ConnectException connEx) {

        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private void run() {
        
        int partitionIndex = 0;
        int attempt = 0;
        while (record.get().VN < 9) {

            sleep(1000);

            int[] currentPartition = new int[1];
            for (int[] a : partitions[partitionIndex]) {
                for (int i : a) {
                    if (id == i) {
                        currentPartition = a;
                        break;
                    }
                }
            }

            // TODO: partition network

            int vr = votingRound.get();
            vr++;
            votingRound.set(vr);

            int pass;
            int fail;
            do {
                pass = 0;
                fail = 0;
                synchronized (votes) {
                    for (FileRecord fr : votes) {
                        if (fr == null)
                            continue;

                        if (fr.VN == vr + 1)
                            pass++;
                        else
                            fail++;
                    }
                }
                System.out.printf("%d votes received\n", pass + fail);
                sleep(100);
            } while (pass + fail + 1 < currentPartition.length);

            FileRecord fr = record.get();
            if (pass + 1 == currentPartition.length && fr.VN == vr + 1) {
                fr.VN++;
                fr.RU = pass + 1;
                if (currentPartition.length % 2 == 0)
                    fr.DS = currentPartition[0];
                else
                    fr.DS = null;
                record.set(new FileRecord(fr.VN, fr.RU, fr.DS));

                try {
                    // TODO: file should be in a directory only for that node
                    Files.write(Paths.get("f"), ("\n" + fr.toString()).getBytes(), StandardOpenOption.APPEND);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

            }
            
            System.out.printf("VN: %d, RU: %d, DS: %c\n", fr.VN, fr.RU, (char) (fr.DS + 'a' - 1));
            
            attempt++;
            if (attempt % 2 == 0)
                partitionIndex++;

            synchronized (votes) {
                for (int i = 0; i < votes.length; i++)
                    votes[i] = null;
            }
            
        }

    }

    public static void main(String[] args) {
        new Node(Integer.valueOf(args[0]));
    }

}