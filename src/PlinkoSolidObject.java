public class PlinkoSolidObject extends PlinkoObject{

    //Value which represents an infinite lifetime
    public static final int INF_LIFETIME = -1;

    //The number of board updates before a placed pin is deleted
    public static final int PLACED_PIN_LIFETIME = 5;

    //The number of "turns" this object has until it is destroyed
    private long lifeTime;
    private SolidType type;



    public enum SolidType {
        BOARD_PIN(DisplayedGraphics.CHAR_BOARD_PIN, JsonGraphics.CHAR_BOARD_PIN),
        PLACED_PIN(DisplayedGraphics.CHAR_PLACED_PIN, JsonGraphics.CHAR_PLACED_PIN),
        WALL(DisplayedGraphics.CHAR_WALL, JsonGraphics.CHAR_WALL),
        BALL_SOLIDIFIED(DisplayedGraphics.CHAR_BALL_SOLIDIFIED, JsonGraphics.CHAR_BALL_SOLIDIFIED);

        private final char jsonChar;
        private final char displayedChar;

        SolidType(char displayedChar, char jsonChar) { this.displayedChar = displayedChar; this.jsonChar = jsonChar; }

        public static SolidType fromChar(char character) {
            for (SolidType solidType : SolidType.values()) {
                if (solidType.displayedChar == character || solidType.jsonChar == character) {
                    return solidType;
                }
            }
            throw new IllegalArgumentException("Invalid plinko object: '%c'".formatted(character));
        }

        public char getDisplayedChar() { return displayedChar; }
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

    //Freezes the timer
    public void freezeTimer() {
        lifeTime = INF_LIFETIME;
    }

    //returns false if lifeTime is expired
    public boolean isAlive() {
        return (lifeTime == INF_LIFETIME || lifeTime > 0);
    }

    @Override
    public char getRepresentativeChar() { return type.getDisplayedChar(); }

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
