import java.net.ServerSocket;
import java.net.Socket;

public class Node {
    private int id;
    private final String ip = "10.176.69.";
    private final int startIp = 32;
    private final int numNodes = 1;
    private final NodeClientThread[] clientThreads;
    private final NodeServerThread[] serverThreads;

    public Node(int id) {
        this.id = id;
        this.clientThreads = new NodeClientThread[numNodes];
        this.serverThreads = new NodeServerThread[numNodes];

        startServers();
        startClients();
        run();

    }

    private void startServers() {
    
        System.out.printf("node %d starting servers\n", id);
        
        try {
            ServerSocket serverSocket = new ServerSocket(5056);
            serverSocket.setSoTimeout(0);
            for (int i = 0; i < numNodes; i++) {
                serverThreads[i] = new NodeServerThread(serverSocket, id);
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
            try {
                System.out.println("connecting to " + ip + String.valueOf(ip_suffix));
                Socket s = new Socket(ip + String.valueOf(ip_suffix), 5056);
                int server_id = ip_suffix - (startIp - 1);
                clientThreads[i] = new NodeClientThread(s, id, server_id);
                clientThreads[i].start();
                i++;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        

    }

    private void run() {
        
        while (true) {
            // main loop
        }

    }

    public static void main(String[] args) {
        new Node(Integer.valueOf(args[0]));
    }

}