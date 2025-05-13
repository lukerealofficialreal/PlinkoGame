/// Tutorial code

package MicroRaftExample;

import io.microraft.statemachine.StateMachine;

import java.util.List;
import java.util.function.Consumer;

public class AtomicRegister
        implements StateMachine {

    @Override
    public Object runOperation(long commitIndex, Object operation) {
        if (operation instanceof NewTermOperation) {
            return null;
        }

        throw new IllegalArgumentException("Invalid operation: " + operation + " at commit index: " + commitIndex);
    }

    @Override
    public Object getNewTermOperation() {
        return new NewTermOperation();
    }

    @Override
    public void takeSnapshot(long commitIndex, Consumer<Object> snapshotChunkConsumer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void installSnapshot(long commitIndex, List<Object> snapshotChunks) {
        throw new UnsupportedOperationException();
    }


    private static class NewTermOperation {
    }

}