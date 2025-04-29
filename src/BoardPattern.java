//At the start of the game, the board is generated procedurally from combinations of predefined board segments
//which consist of 2 rows of board objects.
//For example, one of those segments might be:
//          |Q             Q     Q----Q-QQ                 Q    Q----Q-|
//          |                    Q------Q                       Q------|
//The segments are cropped/repeated to fill the length of the board, and the patterns shifted left and right a random
//amount to allow for more variety.

public class BoardPattern {
    private static final int NUM_LINES = 2; //The number of lines in the board pattern

    private int id; //integer id unique to this pattern
    private PatternTag[] tags; //identifiers which group this pattern into one or more categories
    private String[] lines; //The first line of the board pattern

    //transformations
    private static final boolean NO_FLIP = false;
    private static final int NO_OFFSET = 0;

    private boolean flip = NO_FLIP;//is the pattern is mirrored horizontally
    private int xOffset = NO_OFFSET; //the amount that each line is shifted to the right

    //No transformation is applied initially
    public BoardPattern (int id, PatternTag[] tags, String[] lines)  {
        this.id = id;
        this.tags = tags;

        if(lines.length != NUM_LINES)
            throw new ArrayIndexOutOfBoundsException();
        this.lines = lines;
    }

    //Converts a string into a PatternTag
    //Returns null if the given string does not represent a corresponding PatternTag
    public static PatternTag strTagToPatternTag(String strTag) {
        try {
            return PatternTag.valueOf(strTag);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
