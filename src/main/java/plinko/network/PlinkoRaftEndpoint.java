
package main.java.plinko.network;

import io.microraft.RaftEndpoint;

import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.requireNonNull;

// - Note raftstore is very barebones and will likely not be useful for our project, but I will implement what I can.
// - Duncan Zaug
import io.microraft.persistence.*;

public final class PlinkoRaftEndpoint
        implements RaftEndpoint, Comparable<PlinkoRaftEndpoint> {

    private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

    /**
     * Returns a new unique Raft endpoint.
     *
     * @return a new unique Raft endpoint
     */
    public static PlinkoRaftEndpoint newEndpoint(String address, int port) {
        return new PlinkoRaftEndpoint(ID_GENERATOR.getAndIncrement(), address, port);
    }


    private final int id;
    private final String address;
    private final int port;

    private PlinkoRaftEndpoint(int id, String address, int port) {
        this.id = id;
        this.address = address;
        this.port = port;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        return id == ((PlinkoRaftEndpoint) o).id;
    }

    @Override
    public String toString() {
        return "LocalRaftEndpoint{" + "id=" + id + '}';
    }

    //Endpoints should be orderable based on the information available to all clients.
    //This is accomplished by appending the port to the address as strings and comparing them
    //The id field is disregarded
    @Override
    public int compareTo(PlinkoRaftEndpoint o) {
        String thisSubject = address + Integer.toString(port);
        String oSubject = ((PlinkoRaftEndpoint)o).address + Integer.toString(((PlinkoRaftEndpoint)o).port);

        return thisSubject.compareTo(oSubject);
    }
}
