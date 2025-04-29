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


import java.util.ArrayList;

public class PlinkoBoard {
    private static final String BOARD_PATTERNS_PATH = System.getProperty("user.dir") + "BoardPatterns.json";
    private static BoardPatternGenerator boardPatternGenerator = new BoardPatternGenerator(BOARD_PATTERNS_PATH);

    private static final int Y_DIM = 22; //The maximum Y height of the board

    private static final int MIN_X_DIM = 10; //The minimum width a board can be
    private static final int X_INC_INTERVAL = 5; //The amount that the board width can increase by

    private int xLen; //The number of tiles in the X dimension (including outer walls)
    private int yLen; //The number of tiles in the Y dimension (including outer walls)

    //the 'time' at which this state exists. Starts at 0 and increments for every full state update
    private int stateNum = 0;

    //The previous state of this object
    //Used to restore to a previous game state
    //
    //If the game is being run locally, this should always be the current state
    private PlinkoBoard validState;

    //Data structure to store permanent board objects
    //Persist when the map resets
    //Should be the same at the start of a game and the end of a game
    //Every object included here should also be included in boardArray
    private ArrayList<PlinkoObject> persistantObjs;

    //2D array of tiles which make up the plinko board
    private Tile<PlinkoObject>[][] boardArr;

    //Creates an empty board of the given size
    public PlinkoBoard(int numPlayers) {
        //Generate the board
        //
        //to generate the board:
        //1. define the dimensions of the board
        //2. read b

        //Get xLen from the number of players
        xLen = boardWidthFromPlayers(numPlayers);
        yLen = Y_DIM;

        //The board state is initially presumed to be valid
        validState = null;

        persistantObjs =

    }

    //Reads the board pattern file, parses the json to obtain a hashmap of board patterns
    //builds the board randomly from these patterns
    //Ensures the top and bottom of the board are neutral tiles
    public static void generateBoard(int xLen, int yLen, ) {
        maxLen =
    }

    //Reads board patterns from file and returns a hashmap
    public void readBoardPatterns() {

    }

    //This function will need fine-tuning and playtesting to determine how the board width should grow as
    //number of player increase
    //TODO: add logic which will grow the board as more players exist.
    public static int boardWidthFromPlayers(int numPlayers) {
        int xDim = 0;

        return Math.max(xDim, MIN_X_DIM);
    }

}
