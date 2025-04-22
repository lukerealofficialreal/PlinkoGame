abstract class PlinkoObject {
    protected static final int SERVER_ID = 0;
    protected static long nextObjId = 0;

    protected int xPos;
    protected int yPos;
    protected int ownerId;

    public PlinkoObject(int xPos, int yPos) {
        this.xPos = xPos;
        this.yPos = yPos;
        this.ownerId = SERVER_ID;
    }

    public PlinkoObject(int xPos, int yPos, int ownerId) {
        this.xPos = xPos;
        this.yPos = yPos;
        this.ownerId = ownerId;
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

    //Returns the char which was chosen to represent this object
    abstract char getRepresentativeChar();

    public static long getNextObjId() {
        return nextObjId++;
    }

}
