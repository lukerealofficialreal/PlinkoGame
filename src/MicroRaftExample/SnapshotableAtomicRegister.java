/// Tutorial code

package MicroRaftExample;

import io.microraft.statemachine.StateMachine;

import java.util.List;
import java.util.function.Consumer;


public class SnapshotableAtomicRegister extends OperableAtomicRegister
        implements StateMachine {

    @Override
    public void takeSnapshot(long commitIndex, Consumer<Object> snapshotChunkConsumer) {
        // put the current value of the atomic register into a snapshot chunk.
        snapshotChunkConsumer.accept(new AtomicRegisterSnapshotChunk(value));
    }

    @Override
    public void installSnapshot(long commitIndex, List<Object> snapshotChunks) {
        if (snapshotChunks.size() != 1) {
            // takeSnapshot() method returns a single snapshot chunk.
            throw new IllegalArgumentException("Invalid snapshot chunks: " + snapshotChunks + " at commit index: " + commitIndex);
        }

        // install the value of the atomic register from the snapshot chunk.
        this.value = ((AtomicRegisterSnapshotChunk) snapshotChunks.get(0)).value;
    }

    private static class AtomicRegisterSnapshotChunk {
        final Object value;

        AtomicRegisterSnapshotChunk(Object value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "AtomicRegisterSnapshotChunk{" + "value='" + value + '\'' + '}';
        }
    }

}