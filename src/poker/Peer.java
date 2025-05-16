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
    private Map<String, List<String>> playerHands = new HashMap<>();
    private Map<String, Boolean> isStanding = new HashMap<>();
    private List<String> dealerHand = new ArrayList<>();

    private int currentTurnIndex = 0;
    private boolean gameStarted = false;
    private boolean dealerDone = false;
    private Set<String> seenMessages = Collections.synchronizedSet(new HashSet<>());
    private boolean handReceived = false;




    
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

        if (seenMessages.contains(msg)) return;
        seenMessages.add(msg);

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
                        synchronized (this) {
                            String combinedSeed = generateSharedSeed(allSeeds);
                            this.deck = shuffledDeck(combinedSeed);
                            deckReady = true;

                            sortedPlayers = new ArrayList<>(readyPeers);
                            Collections.sort(sortedPlayers);
                    
                            for (String player : sortedPlayers) {
                                List<String> hand = new ArrayList<>();
                                hand.add(deck.remove(0));
                                hand.add(deck.remove(0));
                                playerHands.put(player, hand);
                                isStanding.put(player, false);
                            }
                            
                            // Wait briefly to ensure PeerConnections receive HELLO and set remoteId
                            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                            
                            for (String player : sortedPlayers) {
                                if (player.equals(peerId)) {
                                    System.out.println("🂠 Your hand: " + playerHands.get(player));
                                    handReceived = true;
                                } else {
                                    sendPrivateMessage(player, "HAND:" + String.join(",", playerHands.get(player)));
                                }
                            }
                            dealerHand.add(deck.remove(0));
                            dealerHand.add(deck.remove(0));

                            System.out.println("🂠 Dealer shows: " + dealerHand.get(0));
                            System.out.println("🂠 Dealer hidden: " + dealerHand.get(1));


                            // Wait until this peer has their hand before starting turn loop
                            while (!handReceived) {
                                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                            }

                            if (sortedPlayers.get(0).equals(peerId)) {
                                broadcastMessage("TURN:" + sortedPlayers.get(0));
                            }

                        }
                    }

                }
            }
        }        
        else if (msg.startsWith("TURN:")) {
            String currentPlayer = msg.substring(5).trim();
            System.out.println("🔁 It is " + currentPlayer + "'s turn");
        
            if (currentPlayer.equals(peerId)) {
                System.out.println("👉 Your turn! Type: hit or stand");
            }
        }
        else if (msg.startsWith("MOVE:")) {
            synchronized (this) {
                if (!deckReady || deck == null) {
                    System.out.println("⚠️ Deck not ready. Ignoring move: " + msg);
                    return;
                }
        
                String[] parts = msg.split(":", 3);
                String fromId = parts[1];
                String action = parts[2];
        
                // ❗ Only process move if it's the sender's turn
                if (!sortedPlayers.get(currentTurnIndex).equals(fromId)) {
                    System.out.println("⚠️ Ignoring MOVE from out-of-turn player: " + fromId);
                    return;
                }
        
                System.out.println("🎯 " + fromId + " chose: " + action.toUpperCase());
        
                if (action.equalsIgnoreCase("hit")) {
                    if (fromId.equals(peerId)) {
                        String newCard = deck.remove(0);
                        if (!playerHands.containsKey(fromId)) {
                            playerHands.put(fromId, new ArrayList<>());
                        }
                        playerHands.get(fromId).add(newCard);
                            
                        if (handValue(playerHands.get(fromId)) > 21) {
                            System.out.println("💥 You busted!");
                            isStanding.put(fromId, true);
                            advanceTurn();
                        }
                    }
                } else if (action.equalsIgnoreCase("stand")) {
                    isStanding.put(fromId, true);
                    advanceTurn();
                }
            }
        }
        
        else if (msg.startsWith("HITCARD:")) {
            String[] parts = msg.split(":");
            String targetId = parts[1];
            String card = parts[2];
        
            if (targetId.equals(peerId)) {
                synchronized (this) {
                    if (!playerHands.containsKey(peerId)) {
                        playerHands.put(peerId, new ArrayList<>());
                    }
                    playerHands.get(peerId).add(card);
                }
                System.out.println("🂡 You received: " + card);
            } else {
                System.out.println("🂡 " + targetId + " received a card");
            }
        }
        
        else if (msg.startsWith("HAND:")) {
            String[] cards = msg.substring(5).split(",");
            List<String> hand = new ArrayList<>();
            Collections.addAll(hand, cards);
        
            synchronized (this) {
                playerHands.put(peerId, hand);
                handReceived = true;
            }
        
            System.out.println("🂠 Your hand: " + hand);
        }
        
        
    }
    

    
    public boolean hasConnectionTo(int port) {
        for (PeerConnection c : connections) {
            if (c.getRemotePort() == port) return true;
        }
        return false;
    }
    
    public void advanceTurn() {
        currentTurnIndex++;
    
        while (currentTurnIndex < sortedPlayers.size() &&
            isStanding.getOrDefault(sortedPlayers.get(currentTurnIndex), false)) {
            currentTurnIndex++;
        }
    
        if (currentTurnIndex >= sortedPlayers.size()) {
            dealerTurn();
        } else {
            broadcastMessage("TURN:" + sortedPlayers.get(currentTurnIndex));
        }
    }
    public void dealerTurn() {
        synchronized (this) {
            if (dealerDone) return;  // ✅ Prevent running more than once
            dealerDone = true;
        }
    
        System.out.println("🧑‍⚖️ Dealer's hand: " + dealerHand);
        int total = handValue(dealerHand);
        while (total < 17) {
            String newCard = deck.remove(0);
            dealerHand.add(newCard);
            System.out.println("🂡 Dealer draws: " + newCard);
            total = handValue(dealerHand);
        }
    
        System.out.println("🎲 Dealer total: " + total);
        evaluateResults();
    }
    
    
    public String generateSharedSeed(Map<String, String> seeds) {
        return seeds.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(Map.Entry::getValue)
            .collect(Collectors.joining());
    }
    public void evaluateResults() {
        int dealerScore = handValue(dealerHand);
        boolean dealerBust = dealerScore > 21;
    
        for (String player : sortedPlayers) {
            int playerScore = handValue(playerHands.get(player));
            boolean playerBust = playerScore > 21;
    
            String result;
            if (playerBust) {
                result = "LOSE";
            } else if (dealerBust || playerScore > dealerScore) {
                result = "WIN";
            } else if (playerScore == dealerScore) {
                result = "PUSH";
            } else {
                result = "LOSE";
            }
    
            System.out.println("🏁 " + player + " result: " + result);
            broadcastMessage("RESULT:" + player + ":" + result);
        }
    }
    public int handValue(List<String> hand) {
        int total = 0;
        int aces = 0;
    
        for (String card : hand) {
            String rank = card;
    
            if (rank.equals("A")) {
                aces++;
                total += 11;
            } else if ("JQK".contains(rank)) {
                total += 10;
            } else {
                total += Integer.parseInt(rank);
            }
        }
    
        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }
    
        return total;
    }
    public void sendPrivateMessage(String targetId, String msg) {
        for (PeerConnection conn : connections) {
            String rid = conn.getRemoteId();
            if (rid != null && rid.equals(targetId)) {
                conn.sendMessage(msg);
            }
        }
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
