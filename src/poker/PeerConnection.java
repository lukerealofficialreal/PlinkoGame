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
    private String remoteId;

    public PeerConnection(Socket socket, Peer peer) throws IOException {
        this.socket = socket;
        this.peer = peer;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }
    public String getRemoteId() {
        return remoteId;
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
                if (inputLine.startsWith("HELLO:")) {
                    String[] parts = inputLine.split(":");
                    if (parts.length >= 2) {
                        this.remoteId = parts[1];  // ✅ Set it here
                    }
                }
                
                peer.receiveMessage(inputLine, socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
                
            }
        } catch (IOException e) {
            System.out.println("Connection lost: " + e.getMessage());
        }
    }
}
