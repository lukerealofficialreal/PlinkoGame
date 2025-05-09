package main.java.plinko.model.records;

import main.java.plinko.model.BoardPattern;

//The record which is sent by the server to the clients to allow them to initialize their boards
public record InitGameRec(
        int numPlayers,
        long randomSeed,
        BoardPattern[] patterns
) {
}
