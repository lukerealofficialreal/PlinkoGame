package main.java.plinko.model.records;

import main.java.plinko.model.PlinkoObject;

public record NewPlinkoObjectRec(
        long stateNum,
        PlinkoObject obj,
        int xPos,
        int yPos
) {}
