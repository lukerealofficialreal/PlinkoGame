/// Tutorial code

package MicroRaftExample;

import io.microraft.RaftEndpoint;
import io.microraft.RaftNode;
import io.microraft.model.message.RaftMessage;
import io.microraft.transport.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.microraft.RaftNodeStatus.TERMINATED;
import static java.util.Objects.requireNonNull;

public final class LocalTransport
        implements Transport {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalTransport.class);


    private final RaftEndpoint localEndpoint;
    private final Map<RaftEndpoint, RaftNode> nodes = new ConcurrentHashMap<>();

    public LocalTransport(RaftEndpoint localEndpoint) {
        this.localEndpoint = requireNonNull(localEndpoint);
    }

    public RaftEndpoint getLocalEndpoint() {
        return localEndpoint;
    }

    @Override
    public void send(RaftEndpoint target, RaftMessage message) {
        if (localEndpoint.equals(target)) {
            throw new IllegalArgumentException(localEndpoint.getId() + " cannot send " + message + " to itself!");
        }

        RaftNode node = nodes.get(target);


        if (node != null) {
            node.handle(message);
        }
    }

    @Override
    public boolean isReachable(RaftEndpoint endpoint) {
        return nodes.containsKey(endpoint);
    }

    public void discoverNode(RaftNode node) {
        RaftEndpoint endpoint = node.getLocalEndpoint();
        if (localEndpoint.equals(endpoint)) {
            throw new IllegalArgumentException(localEndpoint + " cannot discover itself!");
        }

        RaftNode existingNode = nodes.putIfAbsent(endpoint, node);
        if (existingNode != null && existingNode != node && existingNode.getStatus() != TERMINATED) {
            throw new IllegalArgumentException(localEndpoint + " already knows: " + endpoint);
        }
    }

    public void undiscoverNode(RaftNode node) {
        RaftEndpoint endpoint = node.getLocalEndpoint();
        if (localEndpoint.equals(endpoint)) {
            throw new IllegalArgumentException(localEndpoint + " cannot undiscover itself!");
        }

        nodes.remove(node.getLocalEndpoint(), node);
    }
}