public class PlinkoPinObject extends PlinkoObject{

    //Value which represents an infinite lifetime
    private static final int INF_LIFETIME = -1;

    //The number of board updates before a placed pin is deleted
    private static final int PLACED_PIN_LIFETIME = 10;

    private int lifeTime;

    public enum PinType {
        BOARD, PLACED, 
    }

    public PlinkoPinObject(int xPos, int yPos) {
        super(xPos, yPos);
        this.lifeTime = INF_LIFETIME;
    }

    public PlinkoPinObject(int xPos, int yPos, int ownerId) {
        super(xPos, yPos, ownerId);
        if(ownerId != SERVER_ID) {
            this.lifeTime = PLACED_PIN_LIFETIME;
        } else {
            this.lifeTime = INF_LIFETIME;
        }
    }
}
