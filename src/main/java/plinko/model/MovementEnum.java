package main.java.plinko.model;

import java.io.Serializable;

public enum MovementEnum implements Serializable {
    UP(0, 1), DOWN(0, -1), LEFT(-1, 0), RIGHT(1, 0);

    private final Integer x;
    private final Integer y;

    MovementEnum(int x, int y) {
        this.x = x;
        this.y = y;
    }

    //move coordinates in direction
    public int[] newPosition(int[] coordinatesXY) {
        int[] newPosition = new int[2];
        newPosition[0] += coordinatesXY[0] + x;
        newPosition[1] += coordinatesXY[1] + y;
        return newPosition;
    }
}
