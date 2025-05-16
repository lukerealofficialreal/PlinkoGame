/// Tutorial code

package main.java.plinko.network;

import io.microraft.statemachine.StateMachine;
import main.java.plinko.model.PlinkoObject;
import main.java.plinko.model.records.NewPlinkoObjectRec;
// - Note raftstore is very barebones and will likely not be useful for our project, but I will implement what I can.
// - Duncan Zaug
import io.microraft.persistence.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class PlinkoRegister implements StateMachine, Serializable {

    //The target of a replication
    public enum UpdateTarget {
        currState, randSeed, placedObjects, checksum
    }

    @Override
    public Object getNewTermOperation() {
        return new NewTermOperation();
    }

    //IDK why this needs to exist but the tutorial says so
    private static class NewTermOperation {
    }

    /**
     * Marker interface for operations to be executed on the atomic register
     * state machine.
     */
    public interface PlinkoRegisterOperation {
    }

    protected long currState;
    protected long randSeed;
    protected List<NewPlinkoObjectRec> placedObjects = new ArrayList<>();
    protected String checksum;


    public static PlinkoRegisterOperation newSetOperation(UpdateTarget target, Object newState) {
        return new SetOperation(target, newState);
    }

    /**
     * Returns a new operation to get the current value of the atomic register.
     *
     * @return the operation to get the current value of the atomic register
     */
    public static PlinkoRegisterOperation newGetOperation(UpdateTarget target) {
        return new GetOperation(target);
    }

    //Comparison is true if currentState contains all the items in new state, and will set all the items in newState
    public static PlinkoRegisterOperation newCasOperation(UpdateTarget compareTarget, Object currentState,
                                                          UpdateTarget setTarget, Object newState) {
        return new CasOperation(compareTarget, currentState, setTarget, newState);
    }

    @Override
    public Object runOperation(long commitIndex,Object operation) {
        if (operation instanceof SetOperation(UpdateTarget target, Object value)) {
            Object prev;
            switch (target) {
                case UpdateTarget.checksum -> {
                    prev = this.checksum;
                    this.checksum = (String) value;
                    return prev;
                }
                case UpdateTarget.randSeed -> {
                    prev = this.randSeed;
                    this.randSeed = (long) value;
                    return prev;
                }
                case UpdateTarget.placedObjects -> {
                    //'Null' to placed objects clears placed objects
                    //Anything else is added to placed objects
                    prev = this.placedObjects;
                    if(value == null) {
                        this.placedObjects.clear();
                    } else {
                        this.placedObjects.add((NewPlinkoObjectRec) value);
                    }
                    return prev;
                }
                case UpdateTarget.currState -> {
                    prev = this.currState;
                    this.currState = (long) value;
                    return prev;
                }
            };
        } else if (operation instanceof CasOperation(UpdateTarget compareTarget,
                                                     Object currentValue,
                                                     UpdateTarget setTarget,
                                                     Object newValue)) {
            boolean success = switch (compareTarget) {
                case UpdateTarget.currState -> this.currState == (long) currentValue;
                case UpdateTarget.checksum -> this.checksum.equals((String) currentValue);
                case UpdateTarget.randSeed -> this.randSeed == (long) currentValue;

                //TODO: Maybe do this properly. There probably needs to be a newPlinkoObjectRec equals method.
                //      It also might be more useful to send an entire list to compare instead of just an element
                case UpdateTarget.placedObjects -> this.placedObjects.contains((NewPlinkoObjectRec) currentValue);
            };
            if (success) {
                switch (setTarget) {
                    case UpdateTarget.checksum -> this.checksum = (String) newValue;
                    case UpdateTarget.randSeed -> this.randSeed = (long) newValue;
                    case UpdateTarget.placedObjects -> {
                        //'Null' to placed objects clears placed objects
                        //Anything else is added to placed objects
                        if(newValue == null) {
                            this.placedObjects.clear();
                        } else {
                            this.placedObjects.add((NewPlinkoObjectRec) newValue);
                        }
                    }
                    case UpdateTarget.currState -> this.currState = (long) newValue;
                };
            }
            return success;
        } else if (operation instanceof GetOperation(UpdateTarget target)) {
            return switch (target) {
                case UpdateTarget.checksum -> this.checksum;
                case UpdateTarget.randSeed -> this.randSeed;
                case UpdateTarget.placedObjects -> this.placedObjects;
                case UpdateTarget.currState -> this.currState;
            };
        }
        return null;
    }

    private record SetOperation(UpdateTarget target,
                                Object value) implements PlinkoRegisterOperation {

        @Override
            public String toString() {
                return "SetOperation{" + "value='" + value + '\'' + '}';
            }
        }

    private record GetOperation(UpdateTarget target) implements PlinkoRegisterOperation {

        @Override
            public String toString() {
                return "GetOperation{}";
            }
        }

    private record CasOperation(UpdateTarget compareTarget,
                                Object currentValue,
                                UpdateTarget setTarget,
                                Object newValue) implements PlinkoRegisterOperation {
    }

    public void takeSnapshot(long commitIndex, Consumer<Object> snapshotChunkConsumer) {
        // put the current value of the atomic register into a snapshot chunk.
        snapshotChunkConsumer.accept(new SnapshotChunk(currState, randSeed, placedObjects, checksum));
    }

    //TODO: tutorial code only supports snapshotChunks being of size 1. Investigate necessity of larger list sizes.
    public void installSnapshot(long commitIndex, List<Object> snapshotChunks) {

        if (snapshotChunks.size() != 1) {
            // takeSnapshot() method returns a single snapshot chunk.
            throw new IllegalArgumentException("Invalid snapshot chunks: " + snapshotChunks + " at commit index: " + commitIndex);
        }

        // install the value of the atomic register from the snapshot chunk.
        this.currState = ((SnapshotChunk) snapshotChunks.get(0)).currState;
        this.randSeed = ((SnapshotChunk) snapshotChunks.get(0)).randSeed;
        this.placedObjects = new ArrayList<>();
        placedObjects.addAll(((SnapshotChunk) snapshotChunks.get(0)).placedObjects);
        this.checksum = ((SnapshotChunk) snapshotChunks.get(0)).checksum;
    }

    private static class SnapshotChunk {
        protected long currState;
        protected long randSeed;
        protected List<NewPlinkoObjectRec> placedObjects = new ArrayList<>();
        protected String checksum;

        SnapshotChunk(long currState, long randSeed, List<NewPlinkoObjectRec> placedObjects, String checksum) {
            this.currState = currState;
            this.randSeed = randSeed;
            this.placedObjects = new ArrayList<>();
            this.placedObjects.addAll(placedObjects);
            this.checksum = checksum;
        }

        @Override
        public String toString() {
            return "PlinkoRegisterSnapshotChunk:\n" +
                    "\tcurrState = '" + currState + "'\n" +
                    "\trandSeed = '" + randSeed + "'\n" +
                    "\tplacedObjects = '" + placedObjects.toString() + "'\n" +
                    "\tchecksum = '" + checksum + "'";
        }
    }


}
