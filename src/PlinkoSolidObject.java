public class PlinkoSolidObject extends PlinkoObject{

    //Value which represents an infinite lifetime
    public static final int INF_LIFETIME = -1;

    //The number of board updates before a placed pin is deleted
    public static final int PLACED_PIN_LIFETIME = 10;

    //The number of "turns" this object has until it is destroyed
    private long lifeTime;
    private SolidType type;



    public enum SolidType {
        BOARD_PIN(TextGraphics.CHAR_BOARD_PIN),
        PLACED_PIN(TextGraphics.CHAR_PLACED_PIN),
        WALL(TextGraphics.CHAR_WALL),
        BALL_SOLIDIFIED(TextGraphics.CHAR_BALL_SOLIDIFIED);

        private final char repChar;

        SolidType(char repChar) { this.repChar = repChar; }

        public static SolidType fromChar(char character) {
            for (SolidType solidType : SolidType.values()) {
                if (solidType.repChar == character) {
                    return solidType;
                }
            }
            throw new IllegalArgumentException("Invalid plinko object: '%c'".formatted(character));
        }

        public char getRepChar() { return repChar; }
    }

    //Constructor with default ownerId (Object belongs to Server)
    public PlinkoSolidObject(SolidType type) {
        super(SERVER_ID);

        //No objects owned by the server have a lifetime
        this.lifeTime = INF_LIFETIME;
        this.type = type;
    }

    //Constructor with ownerId
    //Pass lifeTime of INF_LIFETIME to give the object an infinite lifetime
    //Pass PLACED_PIN_LIFETIME for the standard lifeTime of a placed pin
    //
    public PlinkoSolidObject(int ownerId, SolidType type) {
        super(ownerId);

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
        super(other.ownerId);
        this.lifeTime = other.lifeTime;
        this.type = other.type;
    }

    //update object to the next state
    //returns the new timer state
    public long updateTimer() {
        if(lifeTime > 0) {
            lifeTime--;
        }
        return lifeTime;
    }

    //returns false if lifeTime is expired

    public boolean isAlive() {
        return (lifeTime == INF_LIFETIME || lifeTime > 0);
    }

    @Override
    public char getRepresentativeChar() { return type.getRepChar(); }

    @Override
    public boolean canOccupy(boolean neutral) {
        if(type == SolidType.BALL_SOLIDIFIED)
            return true;
        else
            return !neutral;
    }

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
