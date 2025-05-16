package main.java.plinko.network;

import main.java.plinko.model.records.InitGameRec;
import main.java.plinko.model.records.MultiStateRequest;
import main.java.plinko.model.records.NewPlinkoObjectRec;
import main.java.plinko.model.records.ValidationRequest;

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

    //Advances the current state
    //
    //sets the state to newState and returns 0 if currState is the current state
    //Otherwise, fails and returns the difference between the given currState and the actual current state
    long advanceState(long currState, long newState);

    //methods to enable server to answer the above method
    //void sendNewObjectsForNextState(List<NewPlinkoObjectRec> objects);

    //A method which contains the object which the client would like to see created in the next state update. These
    //objects are sent to the server. If an object's creation is valid, the server will include it in the next state
    //update. Else, it will be discarded.
    //returns false if the object was attempted to be added to a state which is different from the current state
    //true otherwise
    boolean sendNewObjectToServer(NewPlinkoObjectRec newObjects);

    //method to enable server to answer the above method for each client
    //List<NewPlinkoObjectRec> getRequestedObjectsForNextState();

    //A method which takes an integer hashCode which was created from a previous state update along with the number of
    //the first (inclusive) and last (exclusive) composite states. This method should return true if the state is valid,
    //False otherwise
    boolean validateStateUpdates(ValidationRequest request);

    //method to enable server to answer the above method for each client
    //ValidationRequest getValidationRequest();
    //void answerValidationRequest(boolean valid, int playerId);

    //A method which takes the number of a start state (inclusive) and end state (exclusive) and returns a List which
    //contains a list of all new Objects for each new state from startStateNum to endStateNum
    List<List<NewPlinkoObjectRec>> getNewObjectsForStates(MultiStateRequest request);

    //method to enable server to answer the above method for each client
    //MultiStateRequest getMulitStateRequest();
    //void answerMultiStateRequest(List<List<NewPlinkoObjectRec>> newObjects, int playerId);

    //A method which gets the data necessary to build the initial board state. Can only be called at the start of the game
    InitGameRec getInitState();

    //method to enable server to answer the above method for each client
    void setInitState(InitGameRec init);

}
