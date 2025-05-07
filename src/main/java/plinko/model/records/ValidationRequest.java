package main.java.plinko.model.records;

public record ValidationRequest(
        int playerId, int hash, int startStateNum, int endStateNum
) {
}
