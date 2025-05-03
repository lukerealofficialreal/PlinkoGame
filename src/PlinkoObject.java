public abstract class PlinkoObject extends RespectsNeutral {
    protected static final int SERVER_ID = 0;
    //protected static long nextObjId = 0;

    protected int ownerId;
    protected int containerMaxXLen;

    public PlinkoObject(int containerMaxXLen) {
        this.ownerId = SERVER_ID;
        this.containerMaxXLen = containerMaxXLen;
    }

    public PlinkoObject(int ownerId, int containerMaxXLen) {
        this.ownerId = ownerId;
        this.containerMaxXLen = containerMaxXLen;
    }

// Objects have locations, but no need to store them
//    public int getxPos() {
//        return xPos;
//    }
//    public void setxPos(int xPos) {
//        this.xPos = xPos;
//    }
//
//    public int getyPos() {
//        return yPos;
//    }
//    public void setyPos(int yPos) {
//        this.yPos = yPos;
//    }

    public int getOwnerId() {
        return ownerId;
    }

    //Returns the position of the object on the board as if the board were a continuos string of tiles.
    //Takes the total xLength of the board
    //public int getSequentialPos(int boardXLen) {
    //    return xPos + (boardXLen*yPos);
    //}

    //Returns the char which was chosen to represent this object
    abstract char getRepresentativeChar();


//    public static long getNextObjId() {
//        return nextObjId++;
//    }

}
