package poker;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class PeerConnection implements Runnable {
    private Socket socket;
    private Peer peer;
    private BufferedReader in;
    private PrintWriter out;

    public PeerConnection(Socket socket, Peer peer) throws IOException {
        this.socket = socket;
        this.peer = peer;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    public int getRemotePort() {
        return socket.getPort(); // or remote port
    }
    
    public void sendMessage(String msg) {
        out.println(msg);
    }

    public void run() {
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                peer.receiveMessage(inputLine, socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
            }
        } catch (IOException e) {
            System.out.println("Connection lost: " + e.getMessage());
        }
    }
}
