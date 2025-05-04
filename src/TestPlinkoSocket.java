import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        return objFromClients;
    }

    @Override
    public void sendNewObjectsToServer(List<NewPlinkoObjectRec> newObjects) {
        currState++;
        objFromClients.addAll(newObjects);
    }

    //TODO: implement
    @Override
    public boolean validateStateUpdates(int hashCode, int startStateNum, int endStateNum) {
        return false;
    }

    //TODO: implement
    @Override
    public List<List<NewPlinkoObjectRec>> getNewObjectsForStates(int startStateNum, int endStateNum) {
        return List.of();
    }
}
