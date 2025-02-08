package com.kata.tennisscore.service;

import com.kata.tennisscore.domain.GameStatus;
import com.kata.tennisscore.domain.TennisGame;
import com.kata.tennisscore.domain.TennisPoint;
import com.kata.tennisscore.domain.Winner;
import com.kata.tennisscore.repository.TennisGameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TennisGameService {

    @Autowired
    private TennisGameRepository tennisGameRepository;

    @Transactional(transactionManager = "transactionManager")
    public TennisGame processBallEvent(TennisGame game, String winner) {


        if (winner == null || (!winner.equalsIgnoreCase("A") && !winner.equalsIgnoreCase("B"))) {
            throw new IllegalArgumentException("Invalid ball winner: " + winner);
        }

        if (game.getGameStatus() == GameStatus.FINISHED) {
            return game;
        }

        boolean isPlayerA = winner.equalsIgnoreCase("A");

        if (game.getGameStatus() != GameStatus.FINISHED) {
            System.out.println(game.getScoreBoard());
        }

        // Handle DEUCE and ADVANTAGE scenarios
        if (isDeuce(game)) {
            if (game.getGameStatus() == GameStatus.DEUCE) {
                game.setGameStatus(isPlayerA ? GameStatus.ADVANTAGE_A : GameStatus.ADVANTAGE_B);
                System.out.println("Player " + winner + " has Advantage");
            } else if ((game.getGameStatus() == GameStatus.ADVANTAGE_A && isPlayerA) ||
                    (game.getGameStatus() == GameStatus.ADVANTAGE_B && !isPlayerA)) {
                declareWinner(game, winner);
            } else {
                System.out.println("DEUCE");
                game.setGameStatus(GameStatus.DEUCE);
            }
        } else {
            // 1. Store current score before updating
            TennisPoint previousScore = isPlayerA ? game.getScorePlayerA() : game.getScorePlayerB();

            // 2. Update the score
            if (isPlayerA) {
                game.setScorePlayerA(game.getScorePlayerA().next());
            } else {
                game.setScorePlayerB(game.getScorePlayerB().next());
            }

            // 3. If the player was at 40, the opponent was at 30, and `next()` was called, declare a win
            if (previousScore == TennisPoint.FORTY) {
                declareWinner(game, winner);
            }

            // Ensure game enters DEUCE if both players reach 40
            if (game.getScorePlayerA() == TennisPoint.FORTY && game.getScorePlayerB() == TennisPoint.FORTY) {
                if (game.getGameStatus() != GameStatus.DEUCE) {
                    System.out.println("DEUCE");
                    game.setGameStatus(GameStatus.DEUCE);
                }
            }
        }

        return tennisGameRepository.save(game);
    }



    public void processBallSequence(TennisGame game, String ballSequence) {
        for (char ballWinner : ballSequence.toCharArray()) {
            if (game.getGameStatus() == GameStatus.FINISHED) {
                break;
            }
            // Assign the updated game instance to `game`
            game = processBallEvent(game, String.valueOf(ballWinner));
        }
    }

    private void declareWinner(TennisGame game, String winner) {
        System.out.println("Player " + winner + " wins the game");
        game.setGameStatus(GameStatus.FINISHED);
        game.setWinner(winner.equalsIgnoreCase("A") ? Winner.PLAYER_A : Winner.PLAYER_B);

    }

    private boolean isDeuce(TennisGame game) {
        return game.getScorePlayerA() == TennisPoint.FORTY && game.getScorePlayerB() == TennisPoint.FORTY;
    }

    private boolean hasWon(TennisGame game, boolean isPlayerA) {
        //  If both players are at 40, enforce DEUCE
        if (isDeuce(game)) {
            game.setGameStatus(GameStatus.DEUCE);
            return false;
        }

        //  If a player is at Advantage and wins the next point, they win
        if ((game.getGameStatus() == GameStatus.ADVANTAGE_A && isPlayerA) ||
                (game.getGameStatus() == GameStatus.ADVANTAGE_B && !isPlayerA)) {
            return true;
        }
        // In normal play: if a player reaches FORTY and the other score is less than THIRTY, they win.
        boolean isPlayerAtForty = isPlayerA ? game.getScorePlayerA() == TennisPoint.FORTY
                : game.getScorePlayerB() == TennisPoint.FORTY;
        boolean isOtherBelowThirty = isPlayerA
                ? game.getScorePlayerB().ordinal() < TennisPoint.THIRTY.ordinal()
                : game.getScorePlayerA().ordinal() < TennisPoint.THIRTY.ordinal();
        return isPlayerAtForty && isOtherBelowThirty;
    }
}
