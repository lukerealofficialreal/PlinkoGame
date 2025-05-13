/// Tutorial code


import MicroRaftExample.OperableAtomicRegister;
import io.microraft.Ordered;
import io.microraft.RaftEndpoint;
import io.microraft.RaftNode;
import io.microraft.RaftNodeStatus;
import io.microraft.report.RaftTerm;
import io.microraft.statemachine.StateMachine;
import MicroRaftExample.LocalTransport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.*;
/*
   TO RUN THIS TEST ON YOUR MACHINE:
   $ gh repo clone MicroRaft/MicroRaft
   $ cd MicroRaft && ./mvnw clean test -Dtest=io.microraft.tutorial.OperationCommitTest -DfailIfNoTests=false -Ptutorial
   YOU CAN SEE THIS CLASS AT:
   https://github.com/MicroRaft/MicroRaft/blob/master/microraft-tutorial/src/test/java/io/microraft/tutorial/OperationCommitTest.java
 */
public class OperationCommitTest {

    private List<RaftEndpoint> initialMembers = Arrays
            .asList(OperableAtomicRegister.LocalRaftEndpoint.newEndpoint(), OperableAtomicRegister.LocalRaftEndpoint.newEndpoint(), OperableAtomicRegister.LocalRaftEndpoint.newEndpoint());
    private List<LocalTransport> transports = new ArrayList<>();
    private List<RaftNode> raftNodes = new ArrayList<>();

    @Before
    public void startRaftGroup() {
        for (RaftEndpoint endpoint : initialMembers) {
            RaftNode raftNode = createRaftNode(endpoint);
            raftNode.start();
        }
    }

    @After
    public void terminateRaftGroup() {
        raftNodes.forEach(RaftNode::terminate);
    }

    @Test
    public void testCommitOperation() {
        RaftNode leader = waitUntilLeaderElected();

        String value1 = "value1";
        Ordered<String> result1 = leader.<String>replicate(OperableAtomicRegister.newSetOperation(value1)).join();

        assertTrue(result1.getCommitIndex() > 0);
        assertNull(result1.getResult());

        System.out.println("1st operation commit index: " + result1.getCommitIndex() + ", result: " + result1.getResult());

        String value2 = "value2";
        Ordered<String> result2 = leader.<String>replicate(OperableAtomicRegister.newSetOperation(value2)).join();

        assertTrue(result2.getCommitIndex() > result1.getCommitIndex());
        assertEquals(value1, result2.getResult());

        System.out.println("2nd operation commit index: " + result2.getCommitIndex() + ", result: " + result2.getResult());

        String value3 = "value3";
        Ordered<Boolean> result3 = leader.<Boolean>replicate(OperableAtomicRegister.newCasOperation(value2, value3)).join();

        assertTrue(result3.getCommitIndex() > result2.getCommitIndex());
        assertTrue(result3.getResult());

        System.out.println("3rd operation commit index: " + result2.getCommitIndex() + ", result: " + result3.getResult());

        String value4 = "value4";
        Ordered<Boolean> result4 = leader.<Boolean>replicate(OperableAtomicRegister.newCasOperation(value2, value4)).join();

        assertTrue(result4.getCommitIndex() > result3.getCommitIndex());
        assertFalse(result4.getResult());

        System.out.println("4th operation commit index: " + result4.getCommitIndex() + ", result: " + result4.getResult());

        Ordered<String> result5 = leader.<String>replicate(OperableAtomicRegister.newGetOperation()).join();

        assertTrue(result5.getCommitIndex() > result4.getCommitIndex());
        assertEquals(value3, result5.getResult());

        System.out.println("5th operation commit index: " + result5.getCommitIndex() + ", result: " + result5.getResult());
    }

    private RaftNode createRaftNode(RaftEndpoint endpoint) {
        LocalTransport transport = new LocalTransport(endpoint);
        StateMachine stateMachine = new OperableAtomicRegister();
        RaftNode raftNode = RaftNode.newBuilder().setGroupId("default").setLocalEndpoint(endpoint)
                .setInitialGroupMembers(initialMembers).setTransport(transport)
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

    private RaftNode waitUntilLeaderElected() {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(60);
        while (System.currentTimeMillis() < deadline) {
            RaftEndpoint leaderEndpoint = getLeaderEndpoint();
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

    private RaftEndpoint getLeaderEndpoint() {
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

}