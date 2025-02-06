package com.kata.tennisscore.dto;



import java.util.UUID;

public class BallEventMessage {
    private UUID gameId;
    private int ballNumber;
    private String winner;  // "A" or "B"


    public BallEventMessage() {}

    public BallEventMessage(UUID gameId, int ballNumber, String winner) {
        this.gameId = gameId;
        this.ballNumber = ballNumber;
        this.winner = winner;
    }

    public UUID getGameId() {
        return gameId;
    }
    public void setGameId(UUID gameId) {
        this.gameId = gameId;
    }
    public int getBallNumber() {
        return ballNumber;
    }
    public void setBallNumber(int ballNumber) {
        this.ballNumber = ballNumber;
    }
    public String getWinner() {
        return winner;
    }
    public void setWinner(String winner) {
        this.winner = winner;
    }
}

