package poker;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;;

public class Peer {
    private String peerId;
    private int port;
    private ArrayList<PeerConnection> connections = new ArrayList<>();
    private int expectedPeerCount;
    private String mySeed = UUID.randomUUID().toString();
    private Map<String, String> allSeeds = new TreeMap<>();
    private List<String> deck;
    private boolean deckReady = false;
    private Set<String> readyPeers = new HashSet<>();
    private Set<String> connectedPeerIds = new HashSet<>();
    private Map<String, Integer> knownPeers = new HashMap<>();
    private List<String> sortedPlayers;
    private int currentTurnIndex = 0;
    private int pot = 0;
    private boolean roundActive = false;
    private Map<String, List<String>> holeCards = new HashMap<>();
    private List<String> communityCards = new ArrayList<>();
    private int communityRevealStage = 0; // 0 = not revealed, 1 = flop, 2 = turn, 3 = river
    private int turnsTakenInStage = 0;






    
    public Peer(int port, int expectedPeerCount) {
        Random rand = new Random();
        this.peerId = port + "_" + rand.nextInt(100) + "_" + rand.nextInt(100);
        this.port = port;
        this.expectedPeerCount = expectedPeerCount;
    }
    


    
    public void start() throws IOException {
        // Start listening for new peers
        new Thread(new PeerServer(this, port)).start();
    }

    public void connectToPeer(String host, int peerPort) throws IOException {
        System.out.println("Connecting to " + host + ":" + peerPort);
        Socket socket = new Socket(host, peerPort);
        System.out.println("Connected to peer at " + socket.getRemoteSocketAddress());
    
        PeerConnection conn = new PeerConnection(socket, this);
        connections.add(conn);
        new Thread(conn).start();
    
        conn.sendMessage("HELLO:" + peerId + ":" + port);
    }
    
    public List<PeerConnection> getConnections() {
        return this.connections;
    }
    

    public void receiveMessage(String msg, String from) {

    
        if (msg.startsWith("HELLO:")) {
            String[] parts = msg.split(":");
            if (parts.length >= 3) {
                String otherId = parts[1];
                int otherPort = Integer.parseInt(parts[2]);
    
                String[] addressParts = from.split(":");
                String senderIp = addressParts[0];
    
                if (!connectedPeerIds.contains(otherId)) {
                    connectedPeerIds.add(otherId);
                    knownPeers.put(otherId, otherPort);
                    System.out.println("Registered peer " + otherId + " from " + senderIp + ":" + otherPort);
    
                    // Retry reverse connect
                    boolean connected = false;
                    for (int i = 0; i < 5; i++) {
                        try {
                            Thread.sleep(300);
                            connectToPeer(senderIp, otherPort);
                            connected = true;
                            break;
                        } catch (Exception e) {}
                    }
    
                    // Share the new peer with everyone else
                    broadcastMessage("PEERINFO:" + otherId + ":" + senderIp + ":" + otherPort);

                }
            }
        }
    
        else if (msg.startsWith("PEERINFO:")) {
            String[] parts = msg.split(":");
            if (parts.length >= 4) {
                String otherId = parts[1];
                String ip = parts[2];
                int otherPort = Integer.parseInt(parts[3]);
        
                if (!connectedPeerIds.contains(otherId) && !hasConnectionTo(otherPort)) {
                    try {
                        connectToPeer(ip, otherPort); // ✅ real IP now
                        connectedPeerIds.add(otherId);
                        System.out.println("Connected to shared peer " + otherId + " at " + ip + ":" + otherPort);
                    } catch (IOException e) {
                        System.out.println("Could not connect to shared peer " + otherId);
                    }
                }
            }
        }

        else if (msg.startsWith("READY:")) {
            synchronized (this) {
                String readyId = msg.substring(6).trim();
                if (readyId.isEmpty()) return;
        
                if (readyPeers.add(readyId)) {
                    System.out.println(readyId + " is READY (" + readyPeers.size() + "/" + expectedPeerCount + ")");
        
                    if (!readyId.equals(peerId)) {
                        broadcastMessage("READY:" + readyId);
                    }
        
                    System.out.println("DEBUG: I see " + readyPeers.size() + "/" + expectedPeerCount + " ready");
                    System.out.println("Current READY peers: " + readyPeers);
        
                    if (!deckReady && readyPeers.size() == expectedPeerCount) {
                        deckReady = true;
                        System.out.println("✅ All peers are ready. Broadcasting seed...");
                        broadcastSeed();
                    }
                }
            }
        }
        else if (msg.startsWith("SEED:")) {
            synchronized (this) {
                String[] parts = msg.split(":", 3);
                if (parts.length != 3) return;
        
                String senderId = parts[1];
                String seed = parts[2];
        
                if (!allSeeds.containsKey(senderId)) {
                    allSeeds.put(senderId, seed);
                    allSeeds.putIfAbsent(this.peerId, mySeed);
        
                    if (allSeeds.size() == expectedPeerCount && !deckReady) {
                        String combinedSeed = generateSharedSeed(allSeeds);
                        this.deck = shuffledDeck(combinedSeed);
                        this.deckReady = true;
        
                        sortedPlayers = new ArrayList<>(readyPeers);
                        sortedPlayers.removeIf(x -> x == null); // safety
                        Collections.sort(sortedPlayers);
        
                        // Deal hole cards
                        for (int i = 0; i < sortedPlayers.size(); i++) {
                            List<String> hand = new ArrayList<>(deck.subList(i * 2, i * 2 + 2));
                            holeCards.put(sortedPlayers.get(i), hand);
                        }
        
                        int numPlayers = sortedPlayers.size();
                        int holeEndIndex = numPlayers * 2;
                        communityCards = new ArrayList<>(deck.subList(holeEndIndex, holeEndIndex + 5));
        
                        System.out.println("🂠 Your hole cards: " + holeCards.get(peerId));
                    }
        
                    if (!roundActive) {
                        sortedPlayers = new ArrayList<>(readyPeers);
                        sortedPlayers.removeIf(x -> x == null);
                        Collections.sort(sortedPlayers);
                        currentTurnIndex = 0;
        
                        if (sortedPlayers.get(0).equals(peerId)) {
                            roundActive = true;
                            broadcastMessage("TURN:" + sortedPlayers.get(currentTurnIndex));
                        }
                    }
                }
            }
        }        
        else if (msg.startsWith("TURN:")) {
            String currentPlayer = msg.substring(5).trim();
            System.out.println("🔁 It is " + currentPlayer + "'s turn");
        
            if (currentPlayer.equals(peerId)) {
                System.out.println("👉 Your turn! Type: check, bet <amount>, or fold");
            }
        }
        else if (msg.startsWith("MOVE:")) {
            String[] parts = msg.split(":", 3);
            String fromId = parts[1];
            String action = parts[2];
        
            System.out.println("🎯 " + fromId + " chose: " + action.toUpperCase());
        
            synchronized (this) {
                if (action.startsWith("bet ")) {
                    try {
                        int amount = Integer.parseInt(action.split(" ")[1]);
                        if (sortedPlayers.get(currentTurnIndex).equals(peerId)) {
                            pot += amount;
                            broadcastPot();
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid bet from " + fromId);
                    }
                }
            
                // Advance turn if valid player
                if (sortedPlayers.get(currentTurnIndex).equals(fromId)) {
                    turnsTakenInStage++;
            
                    if (turnsTakenInStage >= sortedPlayers.size()) {
                        turnsTakenInStage = 0;
            
                        if (communityRevealStage == 0) {
                            broadcastMessage("COMMUNITY:FLOP:" + String.join(",", communityCards.subList(0, 3)));
                            communityRevealStage = 1;
                        } else if (communityRevealStage == 1) {
                            broadcastMessage("COMMUNITY:TURN:" + communityCards.get(3));
                            communityRevealStage = 2;
                        } else if (communityRevealStage == 2) {
                            broadcastMessage("COMMUNITY:RIVER:" + communityCards.get(4));
                            communityRevealStage = 3;
                        } else {
                            System.out.println("🏁 All stages revealed.");
                        }
            
                        currentTurnIndex = 0;
                    } else {
                        currentTurnIndex = (currentTurnIndex + 1) % sortedPlayers.size();
                    }
            
                    broadcastMessage("TURN:" + sortedPlayers.get(currentTurnIndex));
                }
            }
            
            
        
            // Advance turn if valid player
            if (sortedPlayers.get(currentTurnIndex).equals(fromId)) {
                turnsTakenInStage++;

                if (turnsTakenInStage >= sortedPlayers.size()) {
                    turnsTakenInStage = 0;

                    // Reveal next stage of community cards
                    if (communityRevealStage == 0) {
                        broadcastMessage("COMMUNITY:FLOP:" + String.join(",", communityCards.subList(0, 3)));
                        communityRevealStage = 1;
                    } else if (communityRevealStage == 1) {
                        broadcastMessage("COMMUNITY:TURN:" + communityCards.get(3));
                        communityRevealStage = 2;
                    } else if (communityRevealStage == 2) {
                        broadcastMessage("COMMUNITY:RIVER:" + communityCards.get(4));
                        communityRevealStage = 3;
                    } else {
                        System.out.println("🏁 All stages revealed.");
                    }

                    // Start next betting round
                    currentTurnIndex = 0;
                } else {
                    currentTurnIndex = (currentTurnIndex + 1) % sortedPlayers.size();
                }

                broadcastMessage("TURN:" + sortedPlayers.get(currentTurnIndex));

            }
        }
        else if (msg.startsWith("POT:")) {
            pot = Integer.parseInt(msg.substring(4).trim());
            System.out.println("💰 Pot updated: " + pot);
        }
        
        
    }
    
    public void revealCommunity(String stage) {
        if (stage.equalsIgnoreCase("flop") && communityRevealStage == 0) {
            broadcastMessage("COMMUNITY:FLOP:" + String.join(",", communityCards.subList(0, 3)));
            communityRevealStage = 1;
        } else if (stage.equalsIgnoreCase("turn") && communityRevealStage == 1) {
            broadcastMessage("COMMUNITY:TURN:" + communityCards.get(3));
            communityRevealStage = 2;
        } else if (stage.equalsIgnoreCase("river") && communityRevealStage == 2) {
            broadcastMessage("COMMUNITY:RIVER:" + communityCards.get(4));
            communityRevealStage = 3;
        } else {
            System.out.println("⚠️ Invalid or duplicate stage.");
        }
    }
    public void broadcastPot() {
        broadcastMessage("POT:" + pot);
    }
    
    public boolean hasConnectionTo(int port) {
        for (PeerConnection c : connections) {
            if (c.getRemotePort() == port) return true;
        }
        return false;
    }
    
    
    public String generateSharedSeed(Map<String, String> seeds) {
        return seeds.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(Map.Entry::getValue)
            .collect(Collectors.joining());
    }
    
    public void sendReady() {
        String readyMsg = "READY:" + peerId;
    
        if (readyPeers.add(peerId)) {
            System.out.println("You are READY.");
            broadcastMessage(readyMsg);
    
            // ✅ Simulate receiving our own READY
            receiveMessage(readyMsg, peerId + ":self");

        }
    }
    
    
    

    public List<String> shuffledDeck(String sharedSeed) {
        List<String> baseDeck = DeckBuilder.getStandardDeck();
        long seedHash = sharedSeed.hashCode();
        Collections.shuffle(baseDeck, new Random(seedHash));
        return baseDeck;
    }


    public void broadcastSeed() {
        String msg = "SEED:" + peerId + ":" + mySeed;
        broadcastMessage(msg);
    
        receiveMessage(msg, peerId + ":self");
    }
    
    

    public void broadcastMessage(String msg) {
        for (PeerConnection conn : connections) {
            conn.sendMessage(msg);
        }
    }
    

    public String getPeerId() {
        return peerId;
    }
    public void sendMove(String action) {
        broadcastMessage("MOVE:" + peerId + ":" + action);
    }
    

}
