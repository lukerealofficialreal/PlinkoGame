import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class PlinkoGame {
    public static JFrame frame = new JFrame("Plinko Game");

    public static List<int[]> clickLocs = new ArrayList<>();
    public static boolean notDisplayed = true;

    public static final long FRAME_DURATION = 1000;
    public static final int MY_NEW_OBJECT_BUFFER_SIZE = 1; //The maximum amount of objects which the user can buffer

    public static void main(String[] args) {
        //System.out.println("Plinko test");

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
        List<NewPlinkoObjectRec> myNewObjectBuffer = new ArrayList<>();

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

            //The player can new objects for a short time before it is time to send updates to the server
            long time = System.currentTimeMillis();
            while(System.currentTimeMillis() - time < FRAME_DURATION) {
                int[] xyLocation;
                try {
                    xyLocation = clickLocs.removeFirst();
                } catch (NoSuchElementException e) {
                    try {
                        //If clickLocs is empty, wait for the player to click on the board.
                        //If the frame passes before any clicks, stop waiting and update the board
                        synchronized (PlinkoGame.class) {
                            //FRAME_DURATION - (System.currentTimeMillis() - time)
                            PlinkoGame.class.wait(FRAME_DURATION - (System.currentTimeMillis() - time));
                        }
                        continue;
                    } catch (InterruptedException d) {
                        System.err.println("Core game loop interrupted!");
                        System.err.println(e.getMessage());
                        break gameLoop;
                    }

                }
                //check if this location corresponds to a valid ball drop location (if this client is the dropper)
                //If so, create a new ball at this location and send it to the server
                if(plinkoBoard.getBalls() > 0 && plinkoBoard.validBallLocation(xyLocation[0], xyLocation[1])) {
                    myNewObjectBuffer.add(new NewPlinkoObjectRec(plinkoBoard.getStateNum(),
                            new PlinkoBallObject(myId, xyLocation[0], xyLocation[1]),
                            xyLocation[0],
                            xyLocation[1]));
                    continue;
                }
                //else, Create pin object
                //check if this location corresponds to a valid ball pin location
                //if so, send pin to the server
                PlinkoSolidObject pin = new PlinkoSolidObject(myId, PlinkoSolidObject.SolidType.PLACED_PIN);
                if(!plinkoBoard.getTileAtPos(xyLocation[0],xyLocation[1]).isOccupied() &&
                        plinkoBoard.getTileAtPos(xyLocation[0],xyLocation[1]).canOccupy(pin)) {
                    myNewObjectBuffer.add(new NewPlinkoObjectRec(plinkoBoard.getStateNum(),
                            pin,
                            xyLocation[0],
                            xyLocation[1]));
                    continue;
                }
            }
            //Send all objects to the server
            if(!myNewObjectBuffer.isEmpty()) {
                testPlinkoSocket.sendNewObjectsToServer(myNewObjectBuffer.removeFirst());
            }

            //Make sure the new object buffer is no longer than it's maximum size
            if(MY_NEW_OBJECT_BUFFER_SIZE < myNewObjectBuffer.size()) {
                myNewObjectBuffer.subList(MY_NEW_OBJECT_BUFFER_SIZE, myNewObjectBuffer.size()).clear();
            }

            //The list of new objects which were created by players for this update
            //should be added to the board this update
            //These objects are already verified to be placeable at their given locations
            List<NewPlinkoObjectRec> newObjectRecs = testPlinkoSocket.getNewObjectsForNextState();

            //add new objects to the board
            for(NewPlinkoObjectRec rec : newObjectRecs) {
                plinkoBoard.addObject(rec.obj(),rec.xPos(),rec.yPos());
            }

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

}
