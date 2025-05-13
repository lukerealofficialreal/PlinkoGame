/// Tutorial code


import MicroRaftExample.LocalTransport;
import MicroRaftExample.OperableAtomicRegister;
import MicroRaftExample.SnapshotableAtomicRegister;
import io.microraft.Ordered;
import io.microraft.QueryPolicy;
import io.microraft.RaftConfig;
import io.microraft.RaftEndpoint;
import io.microraft.RaftNode;
import io.microraft.RaftNodeStatus;
import io.microraft.report.RaftLogStats;
import io.microraft.report.RaftTerm;
import io.microraft.statemachine.StateMachine;
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
import static org.junit.Assert.fail;

/*
   TO RUN THIS TEST ON YOUR MACHINE:
   $ gh repo clone MicroRaft/MicroRaft
   $ cd MicroRaft && ./mvnw clean test -Dtest=io.microraft.tutorial.SnapshotInstallationTest -DfailIfNoTests=false -Ptutorial
   YOU CAN SEE THIS CLASS AT:
   https://github.com/MicroRaft/MicroRaft/blob/master/microraft-tutorial/src/test/java/io/microraft/tutorial/SnapshotInstallationTest.java
 */
public class SnapshotInstallationTest {

    private static final int COMMIT_COUNT_TO_TAKE_SNAPSHOT = 100;


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
    public void testSnapshotInstallation() {
        RaftNode leader = waitUntilLeaderElected();
        RaftNode follower = getAnyNodeExcept(leader.getLocalEndpoint());

        disconnect(leader.getLocalEndpoint(), follower.getLocalEndpoint());

        for (int i = 0; i < COMMIT_COUNT_TO_TAKE_SNAPSHOT; i++) {
            leader.replicate(SnapshotableAtomicRegister.newSetOperation("value" + i)).join();
        }

        assertEquals(1, getRaftLogStats(leader).getTakeSnapshotCount());

        connect(leader.getLocalEndpoint(), follower.getLocalEndpoint());

        eventually(() -> {
            RaftLogStats logStats = getRaftLogStats(follower);
            assertEquals(1, logStats.getInstallSnapshotCount());
            assertEquals(logStats.getCommitIndex(), getRaftLogStats(leader).getCommitIndex());
        });

        eventually(() -> assertEquals(1, getRaftLogStats(follower).getInstallSnapshotCount()));

        Ordered<String> leaderQueryResult = leader.<String>query(SnapshotableAtomicRegister.newGetOperation(),
                QueryPolicy.EVENTUAL_CONSISTENCY, 0).join();

        Ordered<String> followerQueryResult = follower.<String>query(SnapshotableAtomicRegister.newGetOperation(),
                QueryPolicy.EVENTUAL_CONSISTENCY, 0).join();

        assertEquals(followerQueryResult.getCommitIndex(), leaderQueryResult.getCommitIndex());
        assertEquals(followerQueryResult.getResult(), leaderQueryResult.getResult());
    }

    private RaftNode createRaftNode(RaftEndpoint endpoint) {
        RaftConfig config = RaftConfig.newBuilder().setCommitCountToTakeSnapshot(COMMIT_COUNT_TO_TAKE_SNAPSHOT).build();
        LocalTransport transport = new LocalTransport(endpoint);
        StateMachine stateMachine = new SnapshotableAtomicRegister();
        RaftNode raftNode = RaftNode.newBuilder().setGroupId("default").setLocalEndpoint(endpoint)
                .setInitialGroupMembers(initialMembers).setConfig(config).setTransport(transport)
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

    private RaftNode getAnyNodeExcept(RaftEndpoint endpoint) {
        requireNonNull(endpoint);

        return raftNodes.stream().filter(raftNode -> !raftNode.getLocalEndpoint().equals(endpoint)).findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    private void disconnect(RaftEndpoint endpoint1, RaftEndpoint endpoint2) {
        requireNonNull(endpoint1);
        requireNonNull(endpoint2);

        getTransport(endpoint1).undiscoverNode(getNode(endpoint2));
        getTransport(endpoint2).undiscoverNode(getNode(endpoint1));
    }

    private void connect(RaftEndpoint endpoint1, RaftEndpoint endpoint2) {
        requireNonNull(endpoint1);
        requireNonNull(endpoint2);

        getTransport(endpoint1).discoverNode(getNode(endpoint2));
        getTransport(endpoint2).discoverNode(getNode(endpoint1));
    }

    private RaftNode getNode(RaftEndpoint endpoint) {
        requireNonNull(endpoint);

        return raftNodes.stream().filter(raftNode -> raftNode.getLocalEndpoint().equals(endpoint)).findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    private LocalTransport getTransport(RaftEndpoint endpoint) {
        requireNonNull(endpoint);

        return transports.stream().filter(transport -> transport.getLocalEndpoint().equals(endpoint)).findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    private RaftLogStats getRaftLogStats(RaftNode leader) {
        return leader.getReport().join().getResult().getLog();
    }

    private void eventually(AssertTask task) {
        AssertionError error = null;
        long timeoutSeconds = 30;
        long sleepMillis = 200;
        long iterations = TimeUnit.SECONDS.toMillis(timeoutSeconds) / sleepMillis;
        for (int i = 0; i < iterations; i++) {
            try {
                try {
                    task.run();
                    return;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } catch (AssertionError e) {
                error = e;
            }

            try {
                MILLISECONDS.sleep(sleepMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (error != null) {
            throw error;
        }

        fail("eventually() failed without AssertionError!");
    }

    @FunctionalInterface
    public interface AssertTask {
        void run()
                throws Exception;
    }

}
