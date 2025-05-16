//At the start of the game, the board is generated procedurally from combinations of predefined board segments
//which consist of 2 rows of board objects.
//For example, one of those segments might be:
//          |Q             Q     Q----Q-QQ                 Q    Q----Q-|
//          |                    Q------Q                       Q------|
//The segments are cropped/repeated to fill the length of the board, and the patterns shifted left and right a random
//amount to allow for more variety.

package main.java.plinko.model;

import main.java.plinko.Exceptions.MalformedPatternException;
import main.java.plinko.game.CyclicString;
import main.java.plinko.resources.JsonGraphics;

import java.io.Serializable;
import java.util.Arrays;

public class BoardPattern implements Serializable {
    public static final int PATTERN_HEIGHT = 2; //The number of rows in the board pattern

    private final int id; //integer id unique to this pattern
    private final PatternTag[] tags; //identifiers which group this pattern into one or more categories
    private final CyclicString[] lines; //The first line of the board pattern

    //transformations
    private static final int NO_OFFSET = 0;

    private boolean flipped = false;//is the pattern is mirrored horizontally
    private int xOffset = NO_OFFSET; //the amount that each line is shifted to the right

    //No transformation is applied initially
    public BoardPattern (int id, PatternTag[] tags, String[] lines)  {
        this.id = id;
        this.tags = tags;

        if(lines.length != PATTERN_HEIGHT)
            throw new ArrayIndexOutOfBoundsException();
        this.lines = Arrays.stream(lines)
               .map(CyclicString::new)
               .toList().toArray(new CyclicString[0]);
    }

    public BoardPattern(BoardPattern other) {
        this.id = other.id;
        this.tags = other.tags; //Shared reference is acceptable because tags is final

        this.lines = new CyclicString[other.lines.length];
        System.arraycopy(other.lines, 0, this.lines, 0, lines.length);

        this.flipped = other.flipped;
        this.xOffset = other.xOffset;
    }

    //Returns the text representation of this board pattern with no cropping/wrapping
    public String[] getLinesText() {
        String[] transformedLines = new String[lines.length];
        for(int i = 0; i < lines.length; i++) {
            transformedLines[i] = lines[i].getStringWithTransformation(flipped, xOffset);
            lines[i].resetPos();
        }
        return transformedLines;
    }

    //Returns the text representation of this board pattern, wrapped/cropped to the given length
    public String[] getLinesText(int xLength) {
        String[] transformedLines = new String[lines.length];
        for(int i = 0; i < lines.length; i++) {
            transformedLines[i] = lines[i].getStringWithTransformation(xLength, flipped, xOffset);
            lines[i].resetPos();
        }
        return transformedLines;
    }

    //Returns the grid of tiles which make up this board pattern, wrapped/cropped to the given length
    public PlinkoTile[][] getPlinkoTiles(int xLength) {
        //Get the transformed lines of text which represent the tiles on the board
        //As a 2d array of chars
        char[][] chars = Arrays.stream(getLinesText(xLength))
                .map(String::toCharArray)
                .toList().toArray(new char[0][0]);

        PlinkoTile[][] plinkoTileGrid = new PlinkoTile[chars.length][chars[0].length];

        //Traverse from left to right, bottom to top
        for (int i = plinkoTileGrid.length - 1; i >= 0; i--) {
            for (int j = 0; j < plinkoTileGrid[i].length; j++) {
                switch (chars[i][j]) {
                    case JsonGraphics.CHAR_EMPTY:
                        plinkoTileGrid[i][j] = new PlinkoTile(false, null);
                        break;
                    case JsonGraphics.CHAR_EMPTY_NEUTRAL:
                        plinkoTileGrid[i][j] = new PlinkoTile(true, null);
                        break;
                    case JsonGraphics.CHAR_BOARD_PIN, JsonGraphics.CHAR_PLACED_PIN, JsonGraphics.CHAR_WALL, JsonGraphics.CHAR_BALL_SOLIDIFIED:
                        plinkoTileGrid[i][j] = new PlinkoTile(false, new PlinkoSolidObject(PlinkoSolidObject.SolidType.fromChar(chars[i][j])));
                        break;
                    default:
                        throw new MalformedPatternException(chars[i][j]);
                }
            }
        }

        return plinkoTileGrid;
    }

    public PatternTag[] getTags() {
        return tags;
    }

    public int getId() {
        return id;
    }

    public boolean isFlipped() {
        return flipped;
    }
    public void setFlipped(boolean flipState) {
        this.flipped = flipState;
    }

    public int getxOffset() {
        return xOffset;
    }
    public void setxOffset(int xOffset) {
        this.xOffset = xOffset;
    }
}
