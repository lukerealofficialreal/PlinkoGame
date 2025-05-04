import java.util.List;
import java.util.Random;

public class PlinkoBallObject extends PlinkoObject {

    //The ball knows its x, y position so that it can relay that position to the board
    int xPos;
    int yPos;

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
    public MovementEnum movementDecision(List<MovementEnum> directions) {
        if(directions.isEmpty()) {
            return null;
        }
        if(directions.contains(MovementEnum.DOWN))
            return MovementEnum.DOWN;

        if((new Random()).nextBoolean()) {
            if(directions.contains(MovementEnum.LEFT))
                return MovementEnum.LEFT;
            else
                return MovementEnum.RIGHT;
        } else {
            if (directions.contains(MovementEnum.RIGHT))
                return MovementEnum.RIGHT;
            else
                return MovementEnum.LEFT;
        }
    }

    //Updates the ball's position with the given movement
    public void Move(MovementEnum move) {
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
        return TextGraphics.CHAR_BALL;
    }

    @Override
    public boolean canOccupy(boolean neutral) {
        return true;
    }
}
