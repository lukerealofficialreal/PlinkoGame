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


    public static final long FRAME_DURATION = 3000;

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
        frame.getContentPane().setLayout(new FlowLayout());

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

        //Start game loop
        while(plinkoBoard.ballsInPlay()) {
            //Display initial board state
            System.out.printf("New state: %d\n",plinkoBoard.getStateNum());
            updateText(
                    plinkoBoard.getBoardAsText(),
                    plinkoBoard.getStateNum(),
                    plinkoBoard.getBalls(),
                    plinkoBoard.getScore());

            plinkoBoard.nextStateNum();

            //The player can make state updates for a short time before it is time to send updates to the server
            long time = System.currentTimeMillis();
            List<NewPlinkoObjectRec> myNewObjects = new ArrayList<>();
            while(System.currentTimeMillis() - time < FRAME_DURATION) {
                int[] xyLocation;
                synchronized (PlinkoGame.class) {
                    try {
                        xyLocation = clickLocs.removeFirst();
                    } catch (NoSuchElementException e) {
                        continue;
                    }
                }
                //check if this location corresponds to a valid ball drop location (if this client is the dropper)
                //If so, create a new ball at this location and send it to the server
                if(plinkoBoard.validBallLocation(xyLocation[0], xyLocation[1])) {
                    myNewObjects.add(new NewPlinkoObjectRec(plinkoBoard.getStateNum(),
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
                    myNewObjects.add(new NewPlinkoObjectRec(plinkoBoard.getStateNum(),
                            pin,
                            xyLocation[0],
                            xyLocation[1]));
                    continue;
                }
            }
            //Send all objects to the server
            testPlinkoSocket.sendNewObjectsToServer(myNewObjects);


            //The list of new objects which were created by players for this update
            //should be added to the board this update
            //These objects are already verified to be placeable at their given locations
            List<NewPlinkoObjectRec> newObjectRecs = testPlinkoSocket.getNewObjectsForNextState();

            //add new objects to the board
            for(NewPlinkoObjectRec rec : newObjectRecs) {
                plinkoBoard.addObject(rec.obj(),rec.xPos(),rec.yPos());
            }

            //Update the board
            //For reasons beyond my comprehension, this causes a concurrency error despite only being called in this
            //thread (to my knowledge)

            plinkoBoard.updateBoard();

       }
    }

    public static void updateText(String[] strings, long numState, int numBalls, int numScore) {


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
                        synchronized (this) {
                            clickLocs.add(new int[]{xPos, yPos});
                        }
                    }
                });

            }
        }

        frame.getContentPane().removeAll();
        frame.getContentPane().add(masterPanel);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

}
