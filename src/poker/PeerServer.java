package poker;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;


public class PeerServer implements Runnable {
    private Peer peer;
    private int port;

    public PeerServer(Peer peer, int port) {
        this.peer = peer;
        this.port = port;
    }

    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Listening on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                PeerConnection conn = new PeerConnection(clientSocket, peer);
                new Thread(conn).start();
                peer.getConnections().add(conn);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
