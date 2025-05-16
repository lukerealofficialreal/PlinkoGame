package main.java.plinko.network;

import MicroRaftExample.LocalTransport;
import io.microraft.RaftEndpoint;
import io.microraft.RaftNode;
import io.microraft.RaftNodeStatus;
import io.microraft.report.RaftTerm;
import io.microraft.statemachine.StateMachine;
import main.java.plinko.model.records.*;
// - Note raftstore is very barebones and will likely not be useful for our project, but I will implement what I can.
// - Duncan Zaug
import io.microraft.persistence.*;

import java.io.Serializable;
import java.util.*;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class PlinkoSocket implements PlinkoSocketTemplate, Serializable {
    private List<RaftEndpoint> endpoints = new ArrayList<>();
    private List<LocalTransport> transports = new ArrayList<>();
    private List<RaftNode> raftNodes = new ArrayList<>();
    Map<RaftEndpoint, RaftNode> endpointToNode = new HashMap<>();

    private RaftNode myNode;

    public PlinkoSocket(List<EndpointDataRec> endpointData) {
        //Create endpoints from endpoint data
        for (EndpointDataRec d : endpointData) {
            RaftEndpoint endpoint = PlinkoRaftEndpoint.newEndpoint(/*data*/d.address(), d.port());
            endpoints.add(endpoint);
        }
        //Create and start raft nodes for each endpoint
        for (RaftEndpoint e : endpoints) {
            RaftNode raftNode = createRaftNode(e);
            endpointToNode.put(e, raftNode);
            raftNode.start();
        }

        //Get my endpoint
        this.myNode = raftNodes.get(0); //Should be chosen based on the address

    }

    private RaftNode createRaftNode(RaftEndpoint endpoint) {
        //Create transport for node
        //TODO: make transport not local
        LocalTransport transport = new LocalTransport(endpoint);

        //Create state machine for node
        StateMachine stateMachine = new PlinkoRegister();
        RaftNode raftNode = RaftNode.newBuilder().setGroupId("default").setLocalEndpoint(endpoint)
                .setInitialGroupMembers(endpoints).setTransport(transport)
                .setStateMachine(stateMachine).build();

        raftNodes.add(raftNode);
        transports.add(transport);
        enableDiscovery(raftNode, transport);

        return raftNode;
    }

    private void enableDiscovery(RaftNode raftNode, LocalTransport transport) {
        for (int i = 0; i < raftNodes.size(); i++) {
            RaftNode otherNode = raftNodes.get(i);
            if (otherNode != raftNode) {
                transports.get(i).discoverNode(raftNode);
                transport.discoverNode(otherNode);
            }
        }
    }

    //Waits until leader is elected. Returns the leader.
    private RaftNode waitUntilLeaderElected() {
        long deadline = System.currentTimeMillis() + 60000;
        while (System.currentTimeMillis() < deadline) {
            RaftEndpoint leaderEndpoint = getInitLeaderEndpoint();
            if (leaderEndpoint != null) {
                return raftNodes.stream().filter(node -> node.getLocalEndpoint().equals(leaderEndpoint)).findFirst()
                        .orElseThrow(IllegalStateException::new);
            }

            try {
                MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        throw new AssertionError("Could not elect a leader on time!");
    }

    //Every time something needs to be replicated, get the leader
    private RaftEndpoint getInitLeaderEndpoint() {
        RaftEndpoint leaderEndpoint = null;
        int leaderTerm = 0;
        for (RaftNode raftNode : raftNodes) {
            if (raftNode.getStatus() == RaftNodeStatus.TERMINATED) {
                continue;
            }

            RaftTerm term = raftNode.getTerm();
            if (term.getLeaderEndpoint() != null) {
                if (leaderEndpoint == null) {
                    leaderEndpoint = term.getLeaderEndpoint();
                    leaderTerm = term.getTerm();
                } else if (!(leaderEndpoint.equals(term.getLeaderEndpoint()) && leaderTerm == term.getTerm())) {
                    leaderEndpoint = null;
                    break;
                }
            } else {
                leaderEndpoint = null;
                break;
            }
        }
        return leaderEndpoint;
    }

    //Returns the node which is the current leader (assuming one has already been elected)
    private RaftEndpoint getLeaderEndpoint() {
        return myNode.getTerm().getLeaderEndpoint();
    }

    //Returns the node which is the current leader (assuming one has already been elected)
    private RaftNode getLeaderNode() {
        return endpointToNode.get(myNode.getTerm().getLeaderEndpoint());
    }

    //Gets the ID belonging to the node which is owned by this PlinkoSocket
    public int getMyId() {
        return ((PlinkoRaftEndpoint)myNode.getLocalEndpoint()).getId();
    }

    //returns an array of ids corresponding to all endpoints
    //The array will be the same order for all clients that call this method
    public int[] getAllIds() {
        //Must be sorted as endpoints is not guaranteed to be in a particular order
        return endpoints.stream().sorted().map(e -> ((PlinkoRaftEndpoint)e).getId()).mapToInt(i -> i).toArray();
    }

    @Override
    public List<NewPlinkoObjectRec> getNewObjectsForNextState() {
        return List.of();
    }

    @Override
    public void sendNewObjectsForNextState(List<NewPlinkoObjectRec> objects) {

    }

    @Override
    public void sendNewObjectToServer(NewPlinkoObjectRec newObject) {

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
        return new InitGameRec(getLeaderNode().<Long>replicate(
                PlinkoRegister.newGetOperation(PlinkoRegister.UpdateTarget.randSeed)
        ).join().getResult());
    }

    //Only works if myNode is the leader, otherwise does nothing
    @Override
    public void setInitState(InitGameRec init) {
        getLeaderNode().<Long>replicate(
                PlinkoRegister.newSetOperation(PlinkoRegister.UpdateTarget.randSeed, init.randSeed())
        ).join();
    }
}
