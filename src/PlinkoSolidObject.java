public class PlinkoSolidObject extends PlinkoObject{

    //Value which represents an infinite lifetime
    private static final int INF_LIFETIME = -1;

    //The number of board updates before a placed pin is deleted
    public static final int PLACED_PIN_LIFETIME = 10;

    //The number of "turns" this object has until it is destroyed
    private long lifeTime;
    private SolidType type;


    public enum SolidType {
        BOARD_PIN('Q'), PLACED_PIN('@'), WALL('|'), BALL_SOLIDIFIED('X');

        private char repChar;

        SolidType(char repChar) { this.repChar = repChar; }
        public char getRepChar() { return repChar; }
    }

    //Constructor with default ownerId (Object belongs to Server)
    public PlinkoSolidObject(int xPos, int yPos, SolidType type) {
        super(xPos, yPos);

        //No objects owned by the server have a lifetime
        this.lifeTime = INF_LIFETIME;
        this.type = type;
    }

    //Constructor with ownerId
    //Pass lifeTime of INF_LIFETIME to give the object an infinite lifetime
    //Pass PLACED_PIN_LIFETIME for the standard lifeTime of a placed pin
    //
    public PlinkoSolidObject(int xPos, int yPos, int ownerId, SolidType type, long lifeTime) {
        super(xPos, yPos, ownerId);

        if(ownerId != SERVER_ID) {
            this.lifeTime = PLACED_PIN_LIFETIME;
        } else {
            //No server objects have a lifetime
            this.lifeTime = INF_LIFETIME;
        }
        this.type = type;
    }

    //Constructor with ownerId
    public PlinkoSolidObject(PlinkoSolidObject other) {
        super(other.xPos, other.yPos, other.ownerId);
        this.lifeTime = other.lifeTime;
        this.type = other.type;
    }

    //update object to the next state
    //Return int which is somewhat unique to this state update
    public long next_state() {
        if(lifeTime > 0) {
            lifeTime--;
        }

        return lifeTime + objId;
    }

    //returns false if lifeTime is expired

    public boolean isAlive() {
        return (lifeTime == INF_LIFETIME || lifeTime > 0);
    }

    @Override
    public char getRepresentativeChar() { return type.getRepChar(); }

    public long getLifeTime() {
        return lifeTime;
    }
    public void setLifeTime(int lifeTime) {
        this.lifeTime = lifeTime;
    }
    public SolidType getType() {
        return type;
    }
    public void setType(SolidType type) {
        this.type = type;
    }
}
