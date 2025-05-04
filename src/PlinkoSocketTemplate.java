import java.util.List;

//Template which should be implemented by the class which is responsible for sending and receiving state updates between
//client amd sever. This serves as a blueprint which is meant to define the types of data expected by the game while
//remaining agnostic to any implementation of the networking code as well as any forms of the data being sent over
//network.
public interface PlinkoSocketTemplate {

    //A method which returns a list of all new objects which were created by players on this game update.
    //A NewPlinkoObjectRec consists of the number corresponding to this state,
    //an object which extends PlinkoObject,
    //the x position on the board,
    //and the y position on the board.
    List<NewPlinkoObjectRec> getNewObjectsForNextState();

    //A method which contains the objects which the client would like to see created in the next state update. These
    //objects are sent to the server. If an object's creation is valid, the server will include it in the next state
    //update. Else, it will be discarded.
    void sendNewObjectsToServer(List<NewPlinkoObjectRec> newObjects);

    //A method which takes an integer hashCode which was created from a previous state update along with the number of
    //the first (inclusive) and last (exclusive) composite states. This method should return true if the state is valid,
    //False otherwise
    boolean validateStateUpdates(int hashCode, int startStateNum, int endStateNum);

    //A method which takes the number of a start state (inclusive) and end state (exclusive) and returns a List which
    //contains a list of all new Objects for each new state from startStateNum to endStateNum
    List<List<NewPlinkoObjectRec>> getNewObjectsForStates(int startStateNum, int endStateNum);

}
