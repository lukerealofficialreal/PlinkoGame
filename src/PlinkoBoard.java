// Game Rules
//
//In this game, there are two possible roles for each player; Catcher and Ball Runner
//Each player takes a turn as the Ball Runner, while the other players are Catchers.
//Whichever player scores the most points during their turn as Ball Runner wins the game.
//
//The goal of the Ball runner is to send balls into the many score pits located at the bottom of the board.
//This can be accomplished by dropping balls from the many droppers located at the top of the board.
//The ball runner is the only player with this ability.
//
//Spawned balls are affected by gravity and will move downwards for as long as there is room to do so. If a ball encounters
//an obstacle, it will pick a direction (either left or right) and attempt to move in that direction. If the ball has no
//possible moves, it and all pins around it become frozen in place for the rest of the round.
//
//The goal of the catcher is to work together with other catchers to prevent the ball runner from scoring. All players have
//the ability to place temporary pins which will block the ball from falling. The catchers will attempt to use these pins
//to create traps which corner the ball. The ball runner may use their pins to redirect the ball away from these traps.
//
//The ball runner can send a ball every 5 seconds. The number of balls allotted to the ball runner at the start of the
//round, as well as the number of scoring pits/droppers, is dependent on the number of catchers.
//
//When a ball lands in a scoring pit, the pit becomes filled. If all scoring pits become filled, every pit becomes cleared
//and the ball runner gains an extra ball for each scoring pit.
//
//The round ends when the ball runner's last ball is scored or becomes solidified. At the end of the round, the board is
//reset, and the ball runner swaps roles with the next catcher in line.
//
//Once every player has taken a turn as the ball runner, the game is over. The player to achieve the highest score during
//their turn as the ball runner wins.
//
//
//
//The plinko board is the boundary which contains all objects related to the game
//
//The plinko board is sized dynamically based on the number of players at the start of the game.
//The size can increase in intervals of 5.
//
//Certain tiles on the board are marked as Neutral. Empty neutral tiles are represented by the - character.
//Player placed pins cannot be placed on neutral tiles. Neutral tiles are included as part of random board elements as well
//as the first 2 rows below the droppers and above the score pits.
//
//
//At the start of the game, the board is generated procedurally from combinations of predefined board segments
//which consist of 2 rows of board objects.
//For example, one of those segments might be:
//          |Q             Q     Q----Q-QQ                 Q    Q----Q-|
//          |                    Q------Q                       Q------|
//The segments are cropped/repeated to fill the length of the board, and the patterns shifted left and right a random
//amount to allow for more variety.
//
//The top of the board is patterned with droppers and the bottom of the board is patterned with scoring pits
//
//  - Droppers        |||-|
//                    |1--|
//      - A location on the board from which a ball can be spawned.
//      - Exist at the top of the board
//      - They are numbered from top to bottom, left to right.
//      - Comprised of both | and - tiles
//      - The - tile and the number tile cannot be overridden but can be passed through by the ball
//
//  - Scoring Pit |___|  |XXX|
//      - Comprised of 2 walls and 3 _ tiles, which the ball can occupy, but not pins
//      - Exist at the bottom of the board
//      - When the ball occupies a _ tile inside a Scoring pit, it is deleted, the pit is filled with solid X tiles,
//           and a point is scored for the ball runner
//      - If all scoring pits on the board become XXX, they are reset and the ball runner gets an extra ball for each scoring pit
//
/*
An example of what the text representation of a plinko board might look like
<pre>
            |||-||||-||||-||||-||||o||||-||||-||||-||||-||||-||||-||||-|
            |1--||2--||3--||4--||5--||6--||7--||8--||9--||10-||11-||12-|
            |----------------------------------------------------o-----|
            |--Q---------Q----Q---------Q----Q---------Q----Q----Q----Q|
            |                                                          |
            |    Q   Q         Q   Q   Q    Q   Q   Q     Q   Q   Q    |
            |            Q                              Q              |
            |      @                                                Q  |
            | Q    Q Q@Q   Q QQQ Q Q Q Q Q@Q       Q   QQQ Q           |
            |                                                o         |
            |Q    Q   Q   Q   Q   Q   Q Q    Q   Q Q   Q Q   Q   Q  Q  |
            |                                     @                    |
            |   Q Q Q   Q Q   QQQQ QQ Q  Q     Q  Q    Q Q  QQQQ QQ Q  |
            |--------------------------------------    ----------------|
            |Q o     Q     Q    Q      QQQ       Q     Q        Q     Q|
            |  @                         Q                             |
            |Q@            Q     Q----Q-QQ                 Q    Q----Q-|
            |                    Q------Q                       Q------|
            |Q   XXX@@ Q Q   Q  Q  QQ        Q--   Q Q      Q  Q  QQ   |
            |     X                           Q-                       |
            |-Q------oQ-Q-----------------Q-------Q-Q------------------|
            |-----------------------------------o----------------------|
            |___||___||XXX||___||___||___||___||XXX||___||___||___||___|
            ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||


</pre>
 */
//Object storing data structure requirements:
//  - Frequent updates
//  - Frequent copies
//  - Not terribly large
//  For now: ArrayList to store objects in
//
//plinko objects share the following properties:
//  - Position  (int xPos, int yPos)
//      - position consists of 2 integers corresponding the x,y position of the bottom-left corner of the given object,
//          from the bottom left corner of the board
//  - Owner (int id)
//      - An id which represents the owner of this object (usually the creator). Used for color coding and for handling
//          disconnects. Objects owned by the server (permanent pins created at board creation) have the id 0
//
//The plinko board can contain the following objects:
//
//
//  - Solid             Q    @    |    X
//      - Block the ball from moving through them. Have multiple varieties which appear under different conditions:
//      - Board Pins         Q
//          - Spawn with the board. Can also be created via the solidification of placed pins
//      - Placed Pins        @
//          - Placed by players. Only pin variant with a timer before it will be destroyed
//      - Walls              |
//          - flank the walls of the board.
//      - Ball (solidified)  X
//          - Created in place of the ball when it is stuck
//
//
//  - Balls             o
//      - Spawned from a dropper. Moves downward until it is blocked from doing so. If it is blocked, it will
//          choose randomly between going left and going right, and will continue along that path until it is
//          once again able to move downwards or it is blocked again, in which case it will move the opposite direction.
//      - If the ball is unable to move in any direction, the ball is solidified and all pins it is touching become
//          ball solidified as well
//      - If the ball lands in a scoring pit, it is removed from the board
//
//
//
// Game State Recovery via Rollback
//
// This is an overview of the proposed steps which could be taken to synchronize the game state between a client and a server.
// The useful implementation of this functionality necessitates a process which can allow the server to guarantee that
// A client is in sync with the server at a given state. A proposed solution is outlined below.
//
// Game State Validation (Proposed Solution #1)
//      - Let there be a game G, running on both machine X and machine Y.
//      - For every time interval T in which the distance between a given time Ti and Ti+1 is equal for every i,
//           Let there be a state Sx for machine X and Sy for machine Y which are unique for every positive T.
//      - Assume that there exists at least one game state Sv at a time Tv such that Sv == Sx == Sy at time Tv.
//      - Let there be prime integers Qx, Qy which are unique for every state Sx, Sy.
//      - Then, if Qx == Qy, then Sx == Sy.
//
//      - Let there be a pair of game states Sxn, Syn which occur at time Tv+n
//      - Then, for each T from Tv to Tv+n, there must exist both a unique Sx and Sy.
//      - Then, for each T from Tv to Tv+n, there must exist both a unique Qx and Qy.
//      - Let Fx be the product of all Qx from Tv to Tv+n, and Fy be the product of all Qy from Tv to Tv+n
//      - The product of a set of prime numbers is unique for any given set of prime numbers.
//      - Therefore, Fx is unique for any unique Qx from Tv to Tv+n
//      -            Fy is unique for any unique Qx from Tv to Tv+n
//      - Therefore, every unique Fx has a unique set of Qx, and every unique Fy has a unique set of Qy
//      - Therefore, if Fx = Fy, then Qx and Qy from Tv to Tv+n must be equal.
//      - Therefore, if Fx = Fy, then Sx == Sy for all Sx, Sy where T <= T+n, T >= Tv,
//
// The above proof states that, given it is possible to generate a unique prime number for every state transition, and given
// there is some verified starting state where the client and the server have the same state, it is possible to validate
// all states up to the current state by multiplying the primes for each state transition and comparing the results
// obtained by both the client and the server. If they are equal, the states are equal. If they are not equal, the states
// are not equal.
//
// Assigning a unique prime number to each state transition can be accomplished by assigning each prime state transition
// a unique integer and using a map to map the integer to a unique prime. This simplifies the problem to assigning each
// state a unique integer.
//
// Ideas for assigning each state transition a unique integer: ???
//
// Initially, the client receives the state from the server.
// If the client has no last stable state, the server sends over the entire game state
//
// else, it is more efficient to instead rewind the client to its last (confirmed) state and fast forward to the correct state.
// For this purpose, the client and the server both have a value stateTracker which is periodically sent to the server
// alongside the timestamp of the client's last verified state to verify that the client's state is in sync with the server.
// If the client is out of sync with the server, the server will notify the client and send
// a list of times at which objects were created and destroyed since the client's last verified state.
//
// The client will reset to it's last verified state and simulate the game again using the received list of state changes
//
// The client will then send its stateTracker again to verify this corrected state. If this process fails too many times,
// The server will instead send the entire game state


import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PlinkoBoard {
    private static final String BOARD_PATTERNS_PATH = System.getProperty("user.dir") + "/" + "BoardPatterns.json";
    private static BoardPatternGenerator boardPatternGenerator;

    static {
        try {
            boardPatternGenerator = new BoardPatternGenerator(BOARD_PATTERNS_PATH);
        } catch (IOException e) {
            System.err.printf("failed to read '%s.'\n",BOARD_PATTERNS_PATH);
            System.exit(1);
        }
    }

    private static final int Y_DIM = 22; //The maximum Y height of the board

    private static final int MIN_X_DIM = 20; //The minimum width a board can be
    private static final int MAX_X_DIM = 100; //The maximum width a board can be

    private static final int X_INC_INTERVAL = 5; //The amount that the board width can increase by

    private static final int SAFE_ZONE_SIZE = 2; //The number of tiles from the top of the board in which the ball
                                                 //Cannot become stuck and will instead wait
    private final int safeZone; //The y location denoting the first row of tiles inside the safe zone

    private int xLen; //The number of tiles in the X dimension (including outer walls)
    private int yLen; //The number of tiles in the Y dimension (including outer walls)

    //the 'time' at which this state exists. Starts at 0 and increments for every full state update
    private long stateNum = 0;

    private static final int SCORE_INCREMENT = 1;
    private int score = 0;

    //private static final int STARTING_BALLS = 5;
    private int balls;

    //TODO: this value may get out of sync if a ball is next to a ball that has solidified
    private int ballsOnField = 0; //the number of balls on the field

    //The random number generator used to control random events in the game.
    //This should only be used to control random events which are shared between players.
    //All else should use Random class.
    private RandomNumberGenerator random;

    //The patterns used to construct the board
    BoardPattern[] patterns;

    //The previous state of this object
    //Used to restore to a previous game state
    //
    //If the game is being run locally, this should always be the current state
    //TODO: Implement copying the entire board object
    private PlinkoBoard validState;

    //BoardArr which contains only the references to permanent board objects
    private final PlinkoTile[][] initBoardArr;
    private static final int SCORE_PIT_LOCATION = 1;
    private static final int SCORE_PIT_WIDTH = 5;
    private static final int SCORE_PIT_PIT_WIDTH = 3;
    private final PlinkoTile[][] scorePits; //The row of the board which contains the tiles which act as score pits
                                            //Each pit is a group of 3 tiles

    //2D array of tiles which make up the plinko board
    //Each tile can store a plinko object
    private PlinkoTile[][] boardArr;

    //Tiles which contain objects owned by players
    private List<PlinkoTile> playerObjectTiles = new ArrayList<>();

    //Creates an empty board of the given size
    public PlinkoBoard(int numPlayers) {
        //Generate the board
        //
        //to generate the board:
        //1. define the dimensions of the board
        //2. read b

        //Get xLen from the number of players
        this.xLen = boardWidthFromPlayers(numPlayers);
        this.yLen = Y_DIM;

        this.safeZone = (yLen-1)-SAFE_ZONE_SIZE;

        //Declare initial board
        this.patterns = generatePatterns(xLen, yLen);
        this.initBoardArr = boardFromPatterns(patterns);


        //Copy the references to the tiles in initBoardArr to boardArr
        this.boardArr = new PlinkoTile[yLen][xLen];
        for(int i = 0; i < initBoardArr.length; i++) {
            System.arraycopy(initBoardArr[i], 0, boardArr[i], 0, initBoardArr[i].length);
        }

        //Store the locations of each score pit
        PlinkoTile[] scorePitRow = boardArr[yLen-1-SCORE_PIT_LOCATION];

        scorePits = new PlinkoTile[xLen/SCORE_PIT_WIDTH][SCORE_PIT_PIT_WIDTH];

        balls = scorePits.length * 2; //twice as many balls as score pits

        int currPit = 0; //-1 to account for the first tile being a wall
        int currTile = 0;
        boolean newPit = false;
        for(PlinkoTile tile : scorePitRow) {
            if(tile.getObj() == null) {
                scorePits[currPit][currTile] = tile;
                currTile++;
                newPit = true;
            } else {
                if(newPit)
                    currPit++;
                currTile = 0;
                newPit = false;
            }
        }

        //Seed the random number generator
        this.random = new RandomNumberGenerator(new Random().nextInt());

        //The board state is initially presumed to be valid
        this.validState = this; //TODO: This should be a deep copy.
    }

    private BoardPattern[] generatePatterns(int xLen, int yLen) {
        if(yLen%BoardPattern.PATTERN_HEIGHT != 0) {
            throw new IllegalArgumentException("Board height not divisible by pattern height.");
        }
        int numBoardPatterns = yLen/BoardPattern.PATTERN_HEIGHT;

        //There are certain locations on the board which should use specific categories of patterns.
        //For example, some spots of the board can use more empty sets of tiles so the board is not overly crowded.
        //This is accomplished by including the "sparse" tag
        //
        //There are also certain categories of patterns that should only be used at certain locations on the board.
        //For example, the dropper and score pits should only ever appear at the top and bottom of the board
        //respectively. This is accomplished by excluding the "dropper" and "score_pit" tags from all locations except
        //the top and bottom.
        //
        //There arr also certain locations which should not be transformed; both the droppers and the score_pits
        //Use patternTags to restrict which patterns can be used at certain locations on the board.

        //This is not a very scalable or pretty approach to constraining the board layout.
        //If more unique board locations are to be added, this should be refactored into its own
        //BoardConstraint class, which should have an include, exclude, transformed, etc... field for each pattern
        //on the board,
        ArrayList<ArrayList<PatternTag>> include = new ArrayList<ArrayList<PatternTag>>(
                Collections.nCopies(numBoardPatterns,new ArrayList<PatternTag>(
                        List.of(PatternTag.any)
                )));
        ArrayList<ArrayList<PatternTag>> exclude = new ArrayList<ArrayList<PatternTag>>(
                Collections.nCopies(numBoardPatterns,new ArrayList<PatternTag>(
                        Arrays.asList(PatternTag.dropper, PatternTag.score_pit)
                )));
        ArrayList<Boolean> randTransform = new ArrayList<>(Arrays.asList(new Boolean[numBoardPatterns]));
        Collections.fill(randTransform, Boolean.TRUE);

        //Hard code tags for certain areas of the board
        include.set(0, new ArrayList<PatternTag>(List.of(PatternTag.dropper)));
        exclude.set(0, new ArrayList<PatternTag>(List.of()));
        randTransform.set(0, false);

        include.set(1, new ArrayList<PatternTag>(List.of(PatternTag.below_dropper)));
        exclude.set(1, new ArrayList<PatternTag>(List.of()));

        include.set(5, new ArrayList<PatternTag>(List.of(PatternTag.sparse)));
        include.set(numBoardPatterns-4, new ArrayList<PatternTag>(List.of(PatternTag.sparse)));
        include.set(numBoardPatterns-3, new ArrayList<PatternTag>(List.of(PatternTag.standard)));

        include.set(numBoardPatterns-2, new ArrayList<PatternTag>(List.of(PatternTag.above_pit)));
        exclude.set(numBoardPatterns-2, new ArrayList<PatternTag>(List.of()));

        include.set(numBoardPatterns-1, new ArrayList<PatternTag>(List.of(PatternTag.score_pit)));
        exclude.set(numBoardPatterns-1, new ArrayList<PatternTag>(List.of()));
        randTransform.set(numBoardPatterns-1, false);

        //Fill out the board with tiles from random tile patterns
        BoardPattern[] patterns = new BoardPattern[numBoardPatterns];
        for(int i = 0; i < yLen; i+=BoardPattern.PATTERN_HEIGHT)
        {
            //Generate a randomly transformed BoardPattern
            ArrayList<PatternTag> currInclude = include.get(i/BoardPattern.PATTERN_HEIGHT);
            ArrayList<PatternTag> currExclude = exclude.get(i/BoardPattern.PATTERN_HEIGHT);
            Boolean transformed = randTransform.get(i/BoardPattern.PATTERN_HEIGHT);

            BoardPattern pattern;
            if(currExclude.isEmpty()) {
                if(transformed) {
                    pattern = boardPatternGenerator.genRandomPatternWithRandomTransformation(
                            xLen,
                            currInclude.toArray(new PatternTag[0]));
                } else {
                    pattern = boardPatternGenerator.genRandomPattern(
                            currInclude.toArray(new PatternTag[0]));
                }
            } else {
                if(transformed) {
                    pattern = boardPatternGenerator.genRandomPatternWithRandomTransformation(
                            xLen,
                            currInclude.get(0),
                            currExclude.toArray(new PatternTag[0]));
                } else {
                    pattern = boardPatternGenerator.genRandomPattern(
                            currInclude.get(0),
                            currExclude.toArray(new PatternTag[0]));
                }
            }
            //String[] test = pattern.getLinesText();
            //System.out.println(test[0]);
            //System.out.println(test[1]);

            //Get rows of tiles from the board pattern,
            //Add walls to the left and right
            //And add them to the board
            patterns[i/BoardPattern.PATTERN_HEIGHT] = pattern;
        }
        return patterns;
    }

    public PlinkoTile[][] boardFromPatterns(BoardPattern[] patterns) {
        PlinkoTile[][] board = new PlinkoTile[yLen][xLen];

        for(int i = 0; i < yLen; i+=BoardPattern.PATTERN_HEIGHT) {
            PlinkoTile[][] tilePattern = patterns[i/BoardPattern.PATTERN_HEIGHT].getPlinkoTiles(xLen);

            //Each borderWall tile shares a reference to the same wall object.
            PlinkoObject borderWall = new PlinkoSolidObject(PlinkoSolidObject.SolidType.WALL);
            for (int j = i; j < i+BoardPattern.PATTERN_HEIGHT; j++) {
                //Add walls
                tilePattern[j-i][0] = new PlinkoTile(false, borderWall);
                tilePattern[j-i][xLen-1] = new PlinkoTile(false, borderWall);
                System.arraycopy(tilePattern[j-i], 0, board[j], 0, xLen);
            }
        }
        return board;
    }

    //Get the text representation of each line of the board in an array
    public String[] getBoardAsText() {
        String[] strings = new String[boardArr.length];
        for(int i = 0; i < boardArr.length; i++) {
            //Get stream of representative chars from the current row of tiles,
            //Convert each char to a string
            //Join all created strings
            strings[i] = Arrays.stream(boardArr[i])
                    .map(PlinkoTile::getRepresentativeChar)
                    .map(Object::toString)
                    .collect(Collectors.joining());
        }
        return strings;
    }

    //Updates every player owned object on the board
    public void updateBoard() {
        final List<PlinkoTile> playerObjectTilesFinal = List.copyOf(playerObjectTiles); //avoid concurrent modification exception
        for(PlinkoTile tile : playerObjectTilesFinal) {
            if(tile == null) {
                System.out.println("null tile in player objects copy. Skipping...");
                continue;
            }
            else if(tile.getObj() == null) {
                System.out.println("empty tile in player objects copy. Skipping...");
                playerObjectTiles.remove(tile);
                continue;
            }
            updatePlayerOwnedObject(tile);
        }
    }

    public void updatePlayerOwnedObject(PlinkoTile tile) {
        switch(tile.getObj()) {
            case PlinkoSolidObject plinkoSolidObject:
                plinkoSolidObject.updateTimer();
                //Check if the solid object is still alive. If not, get rid of it
                if(!plinkoSolidObject.isAlive()) {
                    tile.clearTile();
                    playerObjectTiles.remove(tile);
                }
                break;
            case PlinkoBallObject plinkoBallObject:
                //Check if the ball can score.
                //if it can, increase the score
                if(plinkoBallObject.getyPos() == 1){
                    scoreBall(plinkoBallObject);
                    playerObjectTiles.remove(tile);
                    break;
                }
                //Prepare a list of movements
                //For each direction, check if it is possible for the ball to move in that direction
                List<MovementEnum> validMovements = new ArrayList<>();
                int[] leftPosition = MovementEnum.LEFT.newPosition(new int[]
                        {plinkoBallObject.getxPos(),plinkoBallObject.getyPos()});
                if(!getTileAtPos(leftPosition[0],leftPosition[1]).isOccupied() &&
                        getTileAtPos(leftPosition[0],leftPosition[1]).canOccupy(plinkoBallObject)){
                    validMovements.add(MovementEnum.LEFT);
                }
                int[] rightPosition = MovementEnum.RIGHT.newPosition(new int[]
                        {plinkoBallObject.getxPos(),plinkoBallObject.getyPos()});
                if(!getTileAtPos(rightPosition[0],rightPosition[1]).isOccupied() &&
                        getTileAtPos(rightPosition[0],rightPosition[1]).canOccupy(plinkoBallObject)){
                    validMovements.add(MovementEnum.RIGHT);
                }
                int[] downPosition = MovementEnum.DOWN.newPosition(new int[]
                        {plinkoBallObject.getxPos(),plinkoBallObject.getyPos()});
                if(!getTileAtPos(downPosition[0],downPosition[1]).isOccupied() &&
                        getTileAtPos(downPosition[0],downPosition[1]).canOccupy(plinkoBallObject)){
                    validMovements.add(MovementEnum.DOWN);
                }
                //Get the movement direction from the ball
                MovementEnum direction = plinkoBallObject.movementDecision(validMovements, random);

                //If no direction was returned, then the ball is stuck. solidify the ball and surounding objects
                if(direction == null) {
                    //Do not solidify the ball if it is in the safe zone
                    if(plinkoBallObject.getyPos() < safeZone) {
                        solidifyObjectsAroundLocation(plinkoBallObject.getxPos(), plinkoBallObject.getyPos());
                        ballsOnField--;
                    }
                    break;
                }

                //Move the ball in the given direction
                plinkoBallObject.move(direction);
                PlinkoTile newTile = getTileAtPos(plinkoBallObject.getxPos(), plinkoBallObject.getyPos());
                newTile.setObj(tile.floatObj());
                playerObjectTiles.remove(tile);
                playerObjectTiles.add(newTile);
                break;
            default:
                throw new IllegalArgumentException("Invalid PlinkoObject!");

        }
    }

    //Turns any player-placed objects at or directly touching the given location into BALL_SOLIDIFIED
    public void solidifyObjectsAroundLocation(int xLoc, int yLoc) {
        int[] target = new int[] {xLoc, yLoc};

        int[][] locations = new int[][] {target,
                MovementEnum.UP.newPosition(target),
                MovementEnum.DOWN.newPosition(target),
                MovementEnum.LEFT.newPosition(target),
                MovementEnum.RIGHT.newPosition(target)};


        for(int[] location : locations) {
            PlinkoTile tile = getTileAtPos(location[0], location[1]);
            if(tile.isOccupied() && tile.getObj().ownerId != PlinkoObject.SERVER_ID) {
                PlinkoSolidObject ballSolidified = new PlinkoSolidObject(
                        tile.getObj().getOwnerId(),
                        PlinkoSolidObject.SolidType.BALL_SOLIDIFIED);
                tile.setObj(ballSolidified);
            }
        }
    }

    //Fills the score pit at the corresponding xPos
    public void scoreBall(PlinkoBallObject ball) {
        int xPos = ball.getxPos();

        PlinkoTile[] scorePit = scorePits[(xPos)/5];

        //In the score pit, place ballSolid into all tiles in the score pit
        PlinkoSolidObject ballSolidified = new PlinkoSolidObject(
                ball.getOwnerId(),
                PlinkoSolidObject.SolidType.BALL_SOLIDIFIED);
        ballSolidified.freezeTimer();

        for(PlinkoTile tile : scorePit) {
            tile.setObj(ballSolidified);
        }

        ballsOnField--;

        incrementScore();

        //if all the score pits are full, clear them and give the ball runner a bonus
        emptyScorePitsIfFull();
    }

    public void emptyScorePitsIfFull() {
        //If any of the score pits is not filled with an object, return
        for(PlinkoTile[] pit : scorePits) {
            for(PlinkoTile tile : pit) {
                if(tile.getObj() == null) {
                    return;
                }
            }
        }
        //else, the score pits must be full;
        //  empty score pits, add ball for each pit
        for(PlinkoTile[] pit : scorePits) {
            for(PlinkoTile tile : pit) {
                tile.clearTile();
            }
            balls++;
        }
    }

    //Returns true if a ball can be placed in the given location
    public boolean validBallLocation(int xLoc, int yLoc) {
        return (yLoc == yLen-1) && !getTileAtPos(xLoc, yLoc).isOccupied();

    }

    //Adds a player created object to the specified location on the board
    //If that object is a ball, subtract 1 from the remaining balls
    //precondition: the location must be valid for the given object
    public void addObject(PlinkoObject obj, int xPos, int yPos) {
        if(obj instanceof PlinkoBallObject) {
            balls--;
            ballsOnField++;
        }

        PlinkoTile tile = getTileAtPos(xPos, yPos);
        playerObjectTiles.add(tile);
        tile.setObj(obj);
    }

    //Gets the tile at the given positon
    public PlinkoTile getTileAtPos(int xPos, int yPos) {
        return boardArr[(yLen-1)-yPos][xPos];
    }

    public void incrementScore() {
        score += SCORE_INCREMENT;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public long getStateNum() {
        return stateNum;
    }

    public void setStateNum(long stateNum) {
        this.stateNum = stateNum;
    }

    public void nextStateNum() {
        this.stateNum++;
    }



    //This function will need fine-tuning and playtesting to determine how the board width should grow as
    //number of player increase
    //TODO: add logic which will grow the board as more players exist.
    //      for now, each player increases board width by 10
    public static int boardWidthFromPlayers(int numPlayers) {
        int xDim = 0;
        xDim += 10*numPlayers;
        return Math.min(Math.max(xDim, MIN_X_DIM), MAX_X_DIM);
    }

    public int getBalls() {
        return balls;
    }

    public void setBalls(int balls) {
        this.balls = balls;
    }

    //Returns true if there are balls remaining or if there are balls on the field
    public boolean ballsInPlay() {
        return ballsOnField > 0 || balls > 0;
    }
}
