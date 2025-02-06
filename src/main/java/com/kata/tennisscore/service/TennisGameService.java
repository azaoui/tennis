package com.kata.tennisscore.service;

import com.kata.tennisscore.domain.GameStatus;
import com.kata.tennisscore.domain.TennisGame;
import com.kata.tennisscore.domain.TennisPoint;
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

        // Handle DEUCE and ADVANTAGE scenarios
        if (isDeuce(game)) {
            if (game.getGameStatus() == GameStatus.DEUCE) {
                game.setGameStatus(isPlayerA ? GameStatus.ADVANTAGE_A : GameStatus.ADVANTAGE_B);
                System.out.println("Player " + winner + " has Advantage");
            } else if ((game.getGameStatus() == GameStatus.ADVANTAGE_A && isPlayerA) ||
                    (game.getGameStatus() == GameStatus.ADVANTAGE_B && !isPlayerA)) {
                declareWinner(game, winner);
                return game;
            } else {
                System.out.println("DEUCE");
                game.setGameStatus(GameStatus.DEUCE);
            }
        }
        // Handle normal score progression
        else {
            if (isPlayerA) {
                game.setScorePlayerA(game.getScorePlayerA().next());
            } else {
                game.setScorePlayerB(game.getScorePlayerB().next());
            }

            System.out.println(game.getScoreBoard());

            // Ensure game enters DEUCE if both players reach 40
            if (game.getScorePlayerA() == TennisPoint.FORTY && game.getScorePlayerB() == TennisPoint.FORTY) {
                if (game.getGameStatus() != GameStatus.DEUCE) {
                    System.out.println("DEUCE");
                    game.setGameStatus(GameStatus.DEUCE);
                }
            }
            //  Check if a player wins the game
            else if (hasWon(game, isPlayerA)) {
                declareWinner(game, winner);
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
            declareWinner(game, isPlayerA ? "A" : "B");
            return true;
        }

        //  If a player reaches 40 and the opponent is below 30, they win immediately
        return (isPlayerA && game.getScorePlayerA() == TennisPoint.FORTY && game.getScorePlayerB().ordinal() < TennisPoint.THIRTY.ordinal()) ||
                (!isPlayerA && game.getScorePlayerB() == TennisPoint.FORTY && game.getScorePlayerA().ordinal() < TennisPoint.THIRTY.ordinal());
    }
}
