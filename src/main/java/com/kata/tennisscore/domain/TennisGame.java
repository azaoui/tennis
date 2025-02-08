package com.kata.tennisscore.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "tennis_game")
public class TennisGame {

    @Id
    @Column(name = "game_id", nullable = false)
    private UUID gameId;


    @Column(name = "ball_sequence", nullable = false)
    private String ballSequence = "";

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "player_a_id")
    private Player playerA;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "player_b_id")
    private Player playerB;

    @Enumerated(EnumType.STRING)
    @Column(name = "score_player_a")
    private TennisPoint scorePlayerA;

    @Enumerated(EnumType.STRING)
    @Column(name = "score_player_b")
    private TennisPoint scorePlayerB;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_status")
    private GameStatus gameStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "winner")
    private Winner winner;


    public TennisGame() {}

    public TennisGame(Player playerA, Player playerB) {
        this.gameId = UUID.randomUUID();
        this.playerA = playerA;
        this.playerB = playerB;
        this.scorePlayerA = TennisPoint.ZERO;
        this.scorePlayerB = TennisPoint.ZERO;
        this.gameStatus = GameStatus.IN_PROGRESS;
        this.ballSequence="";
        this.winner = null; // Initially, no winner
    }

    // Getters and setters.
    public UUID getGameId() {
        return gameId;
    }
    public void setGameId(UUID gameId) {
        this.gameId = gameId;
    }
    public Player getPlayerA() {
        return playerA;
    }
    public void setPlayerA(Player playerA) {
        this.playerA = playerA;
    }
    public Player getPlayerB() {
        return playerB;
    }
    public void setPlayerB(Player playerB) {
        this.playerB = playerB;
    }
    public TennisPoint getScorePlayerA() {
        return scorePlayerA;
    }
    public void setScorePlayerA(TennisPoint scorePlayerA) {
        this.scorePlayerA = scorePlayerA;
    }
    public TennisPoint getScorePlayerB() {
        return scorePlayerB;
    }
    public void setScorePlayerB(TennisPoint scorePlayerB) {
        this.scorePlayerB = scorePlayerB;
    }
    public GameStatus getGameStatus() {
        return gameStatus;
    }
    public void setGameStatus(GameStatus gameStatus) {
        this.gameStatus = gameStatus;
    }

    public Winner getWinner() {
        return winner;
    }

    public void setWinner(Winner winner) {
        this.winner = winner;
    }

    public String getBallSequence() {
        return ballSequence;
    }

    public void setBallSequence(String ballSequence) {
        this.ballSequence = ballSequence;
    }

    // Returns a formatted scoreboard string.
    public String getScoreBoard() {
        String aScore = (scorePlayerA == TennisPoint.ZERO) ? "0" : String.valueOf(scorePlayerA.getValue());
        String bScore = (scorePlayerB == TennisPoint.ZERO) ? "0" : String.valueOf(scorePlayerB.getValue());
        return "Player A : " + aScore + " / Player B : " + bScore;
    }
}
