/*
package main.java.plinko.network;

import main.java.plinko.model.records.InitGameRec;
import main.java.plinko.model.records.MultiStateRequest;
import main.java.plinko.model.records.NewPlinkoObjectRec;
import main.java.plinko.model.records.ValidationRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

//Implements PlinkoSocketTemplate for the purpose of running the game locally
public class TestPlinkoSocket implements PlinkoSocketTemplate{
    //List of updates sent from the client
    List<NewPlinkoObjectRec> objFromClients = new ArrayList<>();


//    //List of lists of updates which might be decoded by the client during normal gameplay
//    List<List<NewPlinkoObjectRec>> exampleUpdates = Arrays.asList(
//            Arrays.asList(), //try an empty first state update
//            Arrays.asList(
//                    new NewPlinkoObjectRec(2, new PlinkoBallObject(1, 3, 22), 3, 22)
//            )
//    );

    int currState = 0;

    @Override
    public List<NewPlinkoObjectRec> getNewObjectsForNextState() {
        //if objects share the same location, remove all but one of the objects
        List<NewPlinkoObjectRec> uniqueObjects =  objFromClients.stream().collect(
                Collectors.groupingBy(p -> Objects.hash(p.xPos(), p.yPos())))
                .values().stream().map(List::getFirst).toList();

        //For each object in newObjects, the server SHOULD remove any new objects which would occupy locations that
        //already contain objects. This test implementation does not


        objFromClients.clear();
        return uniqueObjects;
    }

    @Override
    public void sendNewObjectsForNextState(List<NewPlinkoObjectRec> objects) {

    }

    @Override
    public void sendNewObjectToServer(NewPlinkoObjectRec newObjects) {
        //Discard the previous states new objects (should be a backup in the real implementation)
        //Increment the state
        //Add the new objects for the next state
        currState++;
        if(newObjects.obj() != null) {
            objFromClients.add(newObjects);
        }
    }

    @Override
    public List<NewPlinkoObjectRec> getRequestedObjectsForNextState() {
        return List.of();
    }

    @Override
    public boolean validateStateUpdates(ValidationRequest request) {
        return false;
    }

    @Override
    public ValidationRequest getValidationRequest() {
        return null;
    }

    @Override
    public void answerValidationRequest(boolean valid, int playerId) {

    }

    @Override
    public List<List<NewPlinkoObjectRec>> getNewObjectsForStates(MultiStateRequest request) {
        return List.of();
    }

    @Override
    public MultiStateRequest getMulitStateRequest() {
        return null;
    }

    @Override
    public void answerMultiStateRequest(List<List<NewPlinkoObjectRec>> newObjects, int playerId) {

    }

    @Override
    public InitGameRec getInitState() {
        return null;
    }

    @Override
    public void setInitState(InitGameRec init) {

    }

}
*/