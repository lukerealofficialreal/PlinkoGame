package main.java.plinko.model;

import main.java.plinko.game.RespectsNeutral;

import java.io.Serializable;

public abstract class PlinkoObject extends RespectsNeutral implements Serializable {
    public static final int SERVER_ID = 0;
    //protected static long nextObjId = 0;

    protected int ownerId;

    public PlinkoObject() {
        this.ownerId = SERVER_ID;
    }

    public PlinkoObject(int ownerId) {
        this.ownerId = ownerId;
    }

    //virtual copy constructor
    public abstract PlinkoObject copyOf();

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
