package main.java.plinko.model.records;

public record MultiStateRequest(
        int playerId, int startStateNum, int endStateNum
) {
}
