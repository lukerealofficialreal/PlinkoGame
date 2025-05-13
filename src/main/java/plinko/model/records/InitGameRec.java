package main.java.plinko.model.records;

//The record which is sent by the server to the clients to allow them to initialize their boards
public record InitGameRec(
        long randSeed
) {
}
