package main.java.plinko.game;

import main.java.plinko.model.PlinkoBallObject;
import main.java.plinko.model.PlinkoSolidObject;
import main.java.plinko.model.records.EndpointDataRec;
import main.java.plinko.model.records.InitGameRec;
import main.java.plinko.model.records.NewPlinkoObjectRec;
import main.java.plinko.network.PlinkoSocket;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.*;
import java.util.List;

public class PlinkoGame {
    public static JFrame frame = new JFrame("Plinko Game");

    public static List<int[]> clickLocs = new ArrayList<>();
    public static boolean notDisplayed = true;

    public static final long FRAME_DURATION = 1000;
    public static final int CLICK_BUFFER_SIZE = 1; //The maximum amount of objects which the user can buffer

    public static void main(String[] args) {

        //Create the plinko socket using the known player endpoints
        List<EndpointDataRec> endpointData = getEndpointData();
        PlinkoSocket plinkoSocket = new PlinkoSocket(endpointData);

        //prepare board display
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new OverlayLayout(frame.getContentPane()));
        frame.setLocationRelativeTo(null);
        frame.setAutoRequestFocus(false);

        //Create Game
        int numPlayers = endpointData.size();

        long randSeed = new Random().nextLong();

        //my player id (should be assigned by the server)
        int myId = plinkoSocket.getMyId();

        //Board generation strategy:
        //  Free for all;
        //  Every player generates their own RNG seed
        //  Every player commits their own RNG seed
        //  Every player overrides their own RNG seed with the one currently committed in the state machine
        //  Every player will have the same RNG seed, use it to generate the starting game state
        //  Have a slight pause between setting the RNG seed and getting the RNG seed to account for any desync between clients
        plinkoSocket.setInitState(new InitGameRec(randSeed));
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            System.err.println("interrupted"); //shouldn't happen
            System.exit(1);
        }
        randSeed = plinkoSocket.getInitState().randSeed();

        //Build the board
        PlinkoBoard plinkoBoard = new PlinkoBoard(numPlayers, randSeed);


        //Decide turn order based on obtained random seed
        //First index is ball runner first, second is second, and so on
        int[] playerOrder = plinkoSocket.getAllIds();
        shuffleIntArray(playerOrder, new Random(randSeed));

        int currPlayer = 0;

        //Start game loop
        while(currPlayer < playerOrder.length) {

            //Is this client the ball runner
            boolean isDropper = (myId == playerOrder[currPlayer]);

            //Start round loop
            gameLoop:
            while (plinkoBoard.ballsInPlay()) {
                //Display initial board state
                System.out.printf("New state: %d\n", plinkoBoard.getStateNum());
                displayBoard(
                        plinkoBoard.getBoardAsText(),
                        plinkoBoard.getStateNum(),
                        plinkoBoard.getBalls(),
                        plinkoBoard.getScore());

                //Update the state num
                plinkoBoard.nextStateNum();

                //Update the existing objects on the board
                plinkoBoard.updateBoard();

                NewPlinkoObjectRec myNewObject = waitGetAndVerifyObject(FRAME_DURATION, plinkoBoard, myId);


                //Send new object to the server
                plinkoSocket.sendNewObjectToServer(myNewObject);

                //The list of new objects which were created by players for this update
                //should be added to the board this update
                //These objects are already verified to be placeable at their given locations
                List<NewPlinkoObjectRec> newObjectRecs = plinkoSocket.getNewObjectsForNextState();

                //add new objects to the board
                for (NewPlinkoObjectRec rec : newObjectRecs) {
                    plinkoBoard.addObject(rec.obj(), rec.xPos(), rec.yPos());
                }

            }
            System.out.printf("final state: %d\n", plinkoBoard.getStateNum());
            displayBoard(
                    plinkoBoard.getBoardAsText(),
                    plinkoBoard.getStateNum(),
                    plinkoBoard.getBalls(),
                    plinkoBoard.getScore());

            //have pause between rounds, then reset the board
            try {
                Thread.sleep(1800);
            } catch (InterruptedException e) {
                System.err.println("interrupted"); //shouldn't happen
                System.exit(1);
            }
            //Maybe store the score so it can be displayed later
            currPlayer++; // next player's turn
            plinkoBoard.resetBoard();
        }
    }

    //Waits for the given amount of time and gets the user's new object, or empty NewObjectRec if they made no input
    public static NewPlinkoObjectRec waitGetAndVerifyObject(long milliseconds, PlinkoBoard board, int myId) {
        //The player can new objects for a short time before it is time to send updates to the server
        long time = System.currentTimeMillis();

        //Empty object rec
        NewPlinkoObjectRec newObject = new NewPlinkoObjectRec(board.getStateNum(), null, 0, 0);
        while(System.currentTimeMillis() - time < milliseconds) {
            //Wait until timeout or until a click was made. Do not wait if a click was already buffered
            synchronized(PlinkoGame.class) {
                try {
                    if (clickLocs.isEmpty()) {
                        PlinkoGame.class.wait(Math.max(milliseconds - (System.currentTimeMillis() - time), 0));
                        continue;
                    }
                } catch (InterruptedException e) {
                    System.err.println("wait interrupted!");
                    System.err.println(e.getMessage());
                    System.exit(1);
                }
            }
            //Get the object at the location if it was a valid location
            int[] xyLocation = clickLocs.removeFirst();

            //check if this location corresponds to a valid ball drop location (if this client is the dropper)
            //If so, create a new ball at this location and send it to the server
            if (board.getBalls() > 0 && board.validBallLocation(xyLocation[0], xyLocation[1])) {
                newObject = new NewPlinkoObjectRec(board.getStateNum(),
                        new PlinkoBallObject(myId, xyLocation[0], xyLocation[1]),
                        xyLocation[0],
                        xyLocation[1]);
                break;
            }
            //else, Create pin object
            //check if this location corresponds to a valid ball pin location
            //if so, send pin to the server
            PlinkoSolidObject pin = new PlinkoSolidObject(myId, PlinkoSolidObject.SolidType.PLACED_PIN);
            if (!board.getTileAtPos(xyLocation[0], xyLocation[1]).isOccupied() &&
                    board.getTileAtPos(xyLocation[0], xyLocation[1]).canOccupy(pin)) {
                newObject = new NewPlinkoObjectRec(board.getStateNum(),
                        pin,
                        xyLocation[0],
                        xyLocation[1]);
                break;
            }

            //If there is no new Object, then the last click was in an invalid location. Continue waiting for a valid
            //Click. Else sleep until the time is up
        }
        try {
            {
                Thread.sleep(Math.max(milliseconds - (System.currentTimeMillis() - time), 0));
            }
        } catch (InterruptedException e) {
            System.err.println("wait interrupted!");
            System.err.println(e.getMessage());
            System.exit(1);
        }

        return newObject;
    }

    //TODO: get real endpoint data
    public static List<EndpointDataRec> getEndpointData() {
        return new ArrayList<>(Arrays.asList(
                new EndpointDataRec(
                        "192.168.122.1", 27020
                ), new EndpointDataRec(
                        "192.168.123.1", 27021
                ), new EndpointDataRec(
                        "192.168.124.1", 27022
                ), new EndpointDataRec(
                        "192.168.125.1", 27023
                )
        ));
    }

    //Inplace shuffle on primitive int array
    public static void shuffleIntArray(int[] array, Random random)
    {
        int index;
        int temp;
        for (int i = array.length - 1; i > 0; i--)
        {
            index = random.nextInt(i + 1);
            temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }

    public static void displayBoard(String[] strings, long numState, int numBalls, int numScore) {
        char[][] charArray2D = new char[strings.length][strings[0].length()];
        for (int i = 0; i < strings.length; i++) {
            for (int j = 0; j < strings[i].length(); j++) {
                charArray2D[i][j] = strings[i].charAt(j);
            }
        }

        JPanel masterPanel = new JPanel();
        masterPanel.setLayout(new BoxLayout(masterPanel,BoxLayout.PAGE_AXIS));

        JPanel panel1 = new JPanel(new GridLayout(strings.length, strings[0].length()));
        JLabel state = new JLabel("State: %d".formatted(numState));
        JLabel balls = new JLabel("Balls Remaining: %d".formatted(numBalls));
        JLabel score = new JLabel("Score: %d".formatted(numScore));

        JPanel panel2 = new JPanel(new FlowLayout());
        panel2.add(state);
        panel2.add(balls);
        panel2.add(score);


        //background.add(panel1);
        masterPanel.add(panel2);
        masterPanel.add(panel1);

        JLabel[][] labels = new JLabel[strings.length][strings[0].length()];

        for (int i = 0; i < charArray2D.length; i++) {
            for (int j = 0; j < charArray2D[i].length; j++) {
                labels[i][j] = new JLabel(String.valueOf(charArray2D[i][j]));
                labels[i][j].setFont(new Font(Font.MONOSPACED, Font.PLAIN, 30));
                panel1.add(labels[i][j]);

                int yPos = charArray2D.length-1-i;
                int xPos = j;
                labels[i][j].addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        System.out.printf("clicked x=%d, y=%d\n", xPos, yPos);
                        synchronized (PlinkoGame.class) {
                            clickLocs.add(new int[]{xPos, yPos});
                            if(clickLocs.size() > CLICK_BUFFER_SIZE) {
                                clickLocs.removeFirst();
                            }
                            PlinkoGame.class.notify();
                        }
                    }
                });

            }
        }

        /*
        if(notDisplayed) {
            notDisplayed = false;
        } else {
            frame.getContentPane().remove(1);
        }

         */
        frame.getContentPane().removeAll();
        frame.getContentPane().add(masterPanel);

        frame.pack();
        frame.setVisible(true);
    }

    //plinko board serializer
    // - note: filename must include file extension in the name
    public static void serializeBoard(PlinkoBoard plinkoBoard, String filename) {
        try (FileOutputStream fileOut = new FileOutputStream(filename); ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(plinkoBoard);
            System.out.println("Serialized data is saved in " +  filename);
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    //plink board deserializer
    //give filename that it will deserialize from
    public static PlinkoBoard deserializeBoard(String filename) {
        PlinkoBoard plinkoBoard = null;
        try (FileInputStream fileIn = new FileInputStream(filename); ObjectInputStream in = new ObjectInputStream(fileIn)) {
            plinkoBoard = (PlinkoBoard) in.readObject();
            System.out.println("board deserialized");
            return plinkoBoard;
        } catch (IOException | ClassNotFoundException e) {
            //when error happens it returns null
            e.printStackTrace();
            return plinkoBoard;
        }
    }
}

