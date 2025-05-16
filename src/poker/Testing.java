package poker;

import java.util.Random;
import java.util.Scanner;

public class Testing {
    public static void main(String[] args) throws Exception {
        if (args.length != 2 && args.length != 4) {
            System.out.println("Usage:");
            System.out.println("  First peer: java poker.Testing <myPort> <expectedPeerCount>");
            System.out.println("  Joining peer: java poker.Testing <myPort> <host> <hostPort> <expectedPeerCount>");
            return;
        }

        int myPort = Integer.parseInt(args[0]);
        Peer peer;

        int expectedCount = (args.length == 2) ? Integer.parseInt(args[1]) : Integer.parseInt(args[3]);
        peer = new Peer(myPort, expectedCount);
        peer.start();

        if (args.length == 4) {
            String host = args[1];
            int otherPort = Integer.parseInt(args[2]);
            Thread.sleep(1000);
            peer.connectToPeer(host, otherPort);
        }

        // Allow user to type "ready"
        new Thread(() -> {
            Scanner sc = new Scanner(System.in);
            while (true) {
                String line = sc.nextLine().trim();
                
                if (line.startsWith("bet ") || line.equals("check") || line.equals("fold")) {
                    peer.sendMove(line);
                }
                else if (line.equalsIgnoreCase("ready")) {
                    peer.sendReady();
                }
                else if (line.equalsIgnoreCase("reveal flop")) {
                    peer.revealCommunity("flop");
                }
                else if (line.equalsIgnoreCase("reveal turn")) {
                    peer.revealCommunity("turn");
                }
                else if (line.equalsIgnoreCase("reveal river")) {
                    peer.revealCommunity("river");
                }
                
                
            }
        }).start();
    }
}

