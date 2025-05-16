package main.java.plinko.game;

import main.java.plinko.model.PlinkoBallObject;
import main.java.plinko.model.PlinkoSolidObject;
import main.java.plinko.model.records.NewPlinkoObjectRec;
import main.java.plinko.network.TestPlinkoSocket;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class PlinkoGame {
    public static JFrame frame = new JFrame("Plinko Game");

    public static List<int[]> clickLocs = new ArrayList<>();
    public static boolean notDisplayed = true;

    public static final long FRAME_DURATION = 1000;
    public static final int CLICK_BUFFER_SIZE = 1; //The maximum amount of objects which the user can buffer

    public static void main(String[] args) {
        //System.out.println("Plinko test");

        //Bug: When new objects are created from clicks, the clicked tiles are checked for occupation immediatl

        //test cyclic string transformations
        //String test = "|123456|";
        //CyclicString cycStr = new CyclicString(test);

        //System.out.println(test);
        //System.out.println(cycStr.getString());
        //cycStr.resetPos();
        //System.out.println(cycStr.getString(3));

        //Test board
//        PlinkoBoard plinkoBoard = new PlinkoBoard(3);
//        String[] boardText = plinkoBoard.getBoardAsText();
//        for(String str : boardText) {
//            System.out.println(str);
//        }

        //JFrame frame = new JFrame("JLabel Example");
        //JPanel panel = new JPanel();

        TestPlinkoSocket testPlinkoSocket = new TestPlinkoSocket();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new OverlayLayout(frame.getContentPane()));
        frame.setLocationRelativeTo(null);
        frame.setAutoRequestFocus(false);

        //ImageIcon icon = new ImageIcon(System.getProperty("user.dir") + "/" + "testBackground.png");
        //JLabel label = new JLabel(icon);
        //frame.getContentPane().add(label);

        //Create Game
        int numPlayers = 3;

        //my player id (should be assigned by the server)
        int myId = 1;

        //Is this client the ball runner (should be assigned by the server)
        //Note: bad idea to trust the client to not place balls when they are not the ball runner.
        //      The server should verify that their id matches the current ballrunner
        boolean isDropper = true;

        //Build the board
        PlinkoBoard plinkoBoard = new PlinkoBoard(numPlayers);

        //Buffer which will hold the objects which the player wants to be added
        //The player can only place 1 object per state, so extra objects will be sent one state later

        //Start game loop
        gameLoop:
        while(plinkoBoard.ballsInPlay()) {
            //Display initial board state
            System.out.printf("New state: %d\n",plinkoBoard.getStateNum());
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
            testPlinkoSocket.sendNewObjectsToServer(myNewObject);

            //The list of new objects which were created by players for this update
            //should be added to the board this update
            //These objects are already verified to be placeable at their given locations
            List<NewPlinkoObjectRec> newObjectRecs = testPlinkoSocket.getNewObjectsForNextState();

            //add new objects to the board
            for(NewPlinkoObjectRec rec : newObjectRecs) {
                plinkoBoard.addObject(rec.obj(),rec.xPos(),rec.yPos());
            }

       }
        System.out.printf("final state: %d\n",plinkoBoard.getStateNum());
        displayBoard(
                plinkoBoard.getBoardAsText(),
                plinkoBoard.getStateNum(),
                plinkoBoard.getBalls(),
                plinkoBoard.getScore());
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
