package main.java.plinko.game;

public abstract class RespectsNeutral {
    //returns true if the implementing class can occupy a tile with the given neutrality state
    public abstract boolean canOccupy(boolean neutral);
}

