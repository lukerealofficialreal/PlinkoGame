public abstract class PlinkoObject extends RespectsNeutral {
    protected static final int SERVER_ID = 0;
    //protected static long nextObjId = 0;

    protected int xPos;
    protected int yPos;
    protected int ownerId;
    protected int containerMaxXLen;

    public PlinkoObject(int xPos, int yPos, int containerMaxXLen) {
        this.xPos = xPos;
        this.yPos = yPos;
        this.ownerId = SERVER_ID;
        this.containerMaxXLen = containerMaxXLen;
    }

    public PlinkoObject(int xPos, int yPos, int ownerId, int containerMaxXLen) {
        this.xPos = xPos;
        this.yPos = yPos;
        this.ownerId = ownerId;
        this.containerMaxXLen = containerMaxXLen;
    }

    public int getxPos() {
        return xPos;
    }
    public void setxPos(int xPos) {
        this.xPos = xPos;
    }

    public int getyPos() {
        return yPos;
    }
    public void setyPos(int yPos) {
        this.yPos = yPos;
    }

    public int getOwnerId() {
        return ownerId;
    }

    //Returns the position of the object on the board as if the board were a continuos string of tiles.
    //Takes the total xLength of the board
    public int getSequentialPos(int boardXLen) {
        return xPos + (boardXLen*yPos);
    }

    //Returns the char which was chosen to represent this object
    abstract char getRepresentativeChar();


//    public static long getNextObjId() {
//        return nextObjId++;
//    }

}
