package main.java.plinko.model;

import main.java.plinko.resources.DisplayedGraphics;

//A single tile on the plinkoBoard
//Can be neutral or not neutral
//Can have a obj or be empty
public class PlinkoTile {
    protected final boolean neutral;
    public PlinkoObject obj = null;

    public PlinkoTile(PlinkoTile other) {
        this.neutral = other.neutral;
        this.obj = other.obj.copyOf();
    }

    public PlinkoTile(boolean neutral, PlinkoObject obj) {
        this.neutral = neutral;
        this.obj = obj;
    }

    public PlinkoTile(boolean neutral) {
        this.neutral = neutral;
        this.obj = null;
    }

    //Return true if tile is neutral
    public boolean isNeutral() {
        return neutral;
    }

    //return true if tile has a plinko object
    public boolean isOccupied() {
        return obj != null;
    }

    //Returns true if the object can occupy this tile *IF the tile was empty
    //use isOccupied to check if the tile is empty
    public boolean canOccupy(PlinkoObject obj) {
        return obj.canOccupy(neutral);
    }

    //Removes plinko object from tile and returns it's reference.
    //Useful for moving a plinko object to a different tile without having to set the tile as null
    public PlinkoObject floatObj() {
        PlinkoObject temp = obj;
        obj = null;
        return temp;
    }

    //Removes plinko object from tile by setting obj to null
    public void clearTile() {
        obj = null;
    }

    //returns a reference to the plinko object without removing it from the board. Useful for modifying the object
    //in place
    public PlinkoObject getObj() {
        return obj;
    }

    //Get the representative character of this tile;
    public char getRepresentativeChar() {
        if(isOccupied()) {
            return obj.getRepresentativeChar();
        } else if(isNeutral()) {
            return DisplayedGraphics.CHAR_EMPTY_NEUTRAL;
        } else {
            return DisplayedGraphics.CHAR_EMPTY;
        }
    }

    //sets the current object at this tile to the passed in reference.
    //Overrides any object currently at this location
    //PRECONDITION: The object must be able to occupy this tile given its neutrality state
    //      Use canOccupy to check if the object can occupy this tile
    public void setObj(PlinkoObject obj) {
        this.obj = obj;
    }

}
