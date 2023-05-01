import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

class NodeServerThread extends Thread {
    ObjectOutputStream out;
    ServerSocket server;
    Socket s;
    int clientId;
    final int id;

    public NodeServerThread(ServerSocket server, int id) throws IOException
    {
        this.server = server;
        this.id = id;
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
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        while (true) {
            // node server loop
        }

    }
}