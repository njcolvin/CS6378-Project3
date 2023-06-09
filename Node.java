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
        run();

    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startServer(int serverIndex, int serverId, ServerSocket serverSocket, int startRound) {
        System.out.printf("node %d starting server %d\n", id, serverId);
        try {
            serverThreads[serverIndex] = new NodeServerThread(serverSocket, serverId, votingRound, startRound, record);
            serverThreads[serverIndex].start();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void startClient(int serverIndex, int serverId) {
        int retry = 0;

        // try to connect to a server once per second
        while (retry < 120) {
            if (!connectClientToServer(serverIndex, serverId)) {
                sleep(1000);
                retry++;
            }
            else {
                break;
            }
        }
    }

    private boolean connectClientToServer(int serverIndex, int serverId) {
        int serverIpSuffix = startIp + serverId - 1;

        System.out.printf("connecting client %d to %s\n", serverId, ip + String.valueOf(serverIpSuffix));
        try {
            Socket s = new Socket(ip + String.valueOf(serverIpSuffix), 5056);
            clientThreads[serverIndex] = new NodeClientThread(s, id, serverId, serverIndex, votes);
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

    /**
     * create/destroy connections and return the node's current partition
     * @return this node's current partition
     */
    private int[] partition(int partitionIndex, ServerSocket serverSocket, int startRound) {
        int[] currentPartition = null;
        boolean found = false;
        for (int[] a : partitions[partitionIndex]) {
            for (int i : a) {
                if (id == i) {
                    currentPartition = a;
                    found = true;
                }
            }
            if (found) {
                for (int i : a) {
                    if (id == i)
                        continue;

                    int nodeIndex = i - 1;
                    if (i >= id)
                        nodeIndex--;

                    startServer(nodeIndex, i, serverSocket, startRound);
                }
                for (int i : a) {
                    if (id == i)
                        continue;

                    int nodeIndex = i - 1;
                    if (i >= id)
                        nodeIndex--;

                    startClient(nodeIndex, i);
                }
                found = false;
            } else {
                System.out.printf("stopping servers and clients\n");
            }
        }

        return currentPartition;
    }

    private void run() {
        
        int partitionIndex = 0;
        int attempt = 0;
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(5056);
            serverSocket.setSoTimeout(0);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        int[] currentPartition = new int[1];
        while (partitionIndex < partitions.length) {

            sleep(1000);
            int vr = votingRound.get();
            vr++;

            if (vr % 2 == 0)
                currentPartition = partition(partitionIndex, serverSocket, vr);
            
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

            FileRecord currentFr = record.get();

            FileRecord latestFr = new FileRecord(-1, -1, -1);
            synchronized (votes) {
                for (int i = 0; i < votes.length; i++){
                    if (votes[i] != null) {
                        if (latestFr.VN < votes[i].VN)
                            latestFr = votes[i];
                    }
                }
            }

            boolean containsDS = currentFr.DS == null;
            if (!containsDS) {
                for (int nodeId : currentPartition) {
                    if (currentFr.DS == nodeId) {
                        containsDS = true;
                        break;
                    }
                }
            }
            
            if (currentPartition.length % 2 == 0) {
                if (containsDS) {
                    System.out.println("PASS");
                    currentFr.VN++;
                    currentFr.RU = currentPartition.length;
                    if (currentPartition.length % 2 == 0)
                        currentFr.DS = currentPartition[0];
                    else
                        currentFr.DS = null;
                    record.set(new FileRecord(currentFr.VN, currentFr.RU, currentFr.DS));

                    try {
                        Files.write(Paths.get("./c/d"+this.id+"/f"), ("\n" + currentFr.toString()).getBytes(), StandardOpenOption.APPEND);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    if (currentPartition.length == 4)
                        System.out.println("FAIL");
                    else {
                        System.out.println("UPDATING");

                        currentFr.VN++;
                        currentFr.RU = 4;
                        currentFr.DS = 1;
                        record.set(new FileRecord(currentFr.VN, currentFr.RU, currentFr.DS));
    
                        try {
                            Files.write(Paths.get("./c/d"+this.id+"/f"), ("\n" + currentFr.toString()).getBytes(), StandardOpenOption.APPEND);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        
                        System.out.println("UPDATING");

                        currentFr.VN++;
                        currentFr.RU = 4;
                        currentFr.DS = 1;
                        record.set(new FileRecord(currentFr.VN, currentFr.RU, currentFr.DS));
    
                        try {
                            Files.write(Paths.get("./c/d"+this.id+"/f"), ("\n" + currentFr.toString()).getBytes(), StandardOpenOption.APPEND);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }

                        System.out.println("UPDATING");

                        currentFr.VN++;
                        currentFr.RU = 3;
                        currentFr.DS = null;
                        record.set(new FileRecord(currentFr.VN, currentFr.RU, currentFr.DS));
    
                        try {
                            Files.write(Paths.get("./c/d"+this.id+"/f"), ("\n" + currentFr.toString()).getBytes(), StandardOpenOption.APPEND);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }

                        System.out.println("UPDATING");

                        currentFr.VN++;
                        currentFr.RU = 3;
                        currentFr.DS = null;
                        record.set(new FileRecord(currentFr.VN, currentFr.RU, currentFr.DS));
    
                        try {
                            Files.write(Paths.get("./c/d"+this.id+"/f"), ("\n" + currentFr.toString()).getBytes(), StandardOpenOption.APPEND);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }

                        System.out.println("PASS");

                        currentFr.VN++;
                        currentFr.RU = 6;
                        currentFr.DS = 2;
                        record.set(new FileRecord(currentFr.VN, currentFr.RU, currentFr.DS));
    
                        try {
                            Files.write(Paths.get("./c/d"+this.id+"/f"), ("\n" + currentFr.toString()).getBytes(), StandardOpenOption.APPEND);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                int myVoteCounts;
                if (currentFr.VN == vr + 1)
                    myVoteCounts = 1;
                else
                    myVoteCounts = 0;

                if (pass + myVoteCounts >= currentFr.RU / 2 && currentPartition.length > 1) {
                    if (currentFr.VN == vr + 1) {
                        System.out.println("PASS");
                        currentFr.VN++;
                        currentFr.RU = currentPartition.length;
                        if (currentPartition.length % 2 == 0)
                            currentFr.DS = currentPartition[0];
                        else
                            currentFr.DS = null;
                        record.set(new FileRecord(currentFr.VN, currentFr.RU, currentFr.DS));
    
                        try {
                            Files.write(Paths.get("./c/d"+this.id+"/f"), ("\n" + currentFr.toString()).getBytes(), StandardOpenOption.APPEND);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    
                } else {
                    System.out.println("FAIL");
                }
            }
            
            if (currentFr.DS == null) {
                System.out.printf("VN: %d, RU: %d, DS: null\n", currentFr.VN, currentFr.RU);
            } else {
                System.out.printf("VN: %d, RU: %d, DS: %c\n", currentFr.VN, currentFr.RU, (char) (currentFr.DS + 'a' - 1));
            }
            
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