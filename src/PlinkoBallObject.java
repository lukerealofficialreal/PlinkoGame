import java.util.List;

public class PlinkoBallObject extends PlinkoObject {

    //The ball knows its x, y position so that it can relay that position to the board
    int xPos;
    int yPos;
    MovementEnum lastMove = MovementEnum.DOWN;

    //Constructor with ownerId
    //Pass lifeTime of INF_LIFETIME to give the object an infinite lifetime
    //Pass PLACED_PIN_LIFETIME for the standard lifeTime of a placed pin
    //
    public PlinkoBallObject(int ownerId, int xPos, int yPos) {
        super(ownerId);
        this.xPos = xPos;
        this.yPos = yPos;
    }

    //Constructor with ownerId
    public PlinkoBallObject(PlinkoBallObject other) {
        super(other.ownerId);
        this.xPos = other.xPos;
        this.yPos = other.yPos;
    }

    //Returns one of the valid directions, or null if passed no directions
    public MovementEnum movementDecision(List<MovementEnum> directions, RandomNumberGenerator rand) {
        if(directions.isEmpty()) {
            return null;
        }
        if(directions.contains(MovementEnum.DOWN)) {
            lastMove = MovementEnum.DOWN;
            return MovementEnum.DOWN;
        }

        //randomize the direction
        boolean decision = rand.nextBoolean();

        //If the ball was already going Left/Right, ignore random value and try to go in that direction anyway
        if(lastMove == MovementEnum.LEFT )
            decision = true;
        else if(lastMove == MovementEnum.RIGHT)
            decision = false;

        //TODO: Change to repeatable RNG based on the current state of the game
        if(decision) {
            if(directions.contains(MovementEnum.LEFT)) {
                lastMove = MovementEnum.LEFT;
                return MovementEnum.LEFT;
            }
            else {
                lastMove = MovementEnum.RIGHT;
                return MovementEnum.RIGHT;
            }
        } else {
            if (directions.contains(MovementEnum.RIGHT)) {
                lastMove = MovementEnum.RIGHT;
                return MovementEnum.RIGHT;
            }
            else {
                lastMove = MovementEnum.LEFT;
                return MovementEnum.LEFT;
            }
        }
    }

    //Updates the ball's position with the given movement
    public void move(MovementEnum move) {
        int[] newPos = move.newPosition(new int[] {xPos, yPos});
        xPos = newPos[0];
        yPos = newPos[1];
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

    @Override
    char getRepresentativeChar() {
        return DisplayedGraphics.CHAR_BALL;
    }

    @Override
    public boolean canOccupy(boolean neutral) {
        return true;
    }
}
