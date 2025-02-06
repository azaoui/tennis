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
        // Check for invalid input.
        if (winner == null || !(winner.equalsIgnoreCase("A") || winner.equalsIgnoreCase("B"))) {
            throw new IllegalArgumentException("Invalid ball winner: " + winner);
        }

        // If the game is already finished, do nothing.
        if (game.getGameStatus() == GameStatus.FINISHED) {
            return game;
        }

        boolean isPlayerA = winner.equalsIgnoreCase("A");

        // If both players are at 40, we are in the deuce/advantage region.
        if (game.getScorePlayerA() == TennisPoint.FORTY && game.getScorePlayerB() == TennisPoint.FORTY) {
            if (game.getGameStatus() == GameStatus.DEUCE) {
                // The winner of this ball gets advantage.
                game.setGameStatus(isPlayerA ? GameStatus.ADVANTAGE_A : GameStatus.ADVANTAGE_B);
            } else if ((game.getGameStatus() == GameStatus.ADVANTAGE_A && isPlayerA) ||
                    (game.getGameStatus() == GameStatus.ADVANTAGE_B && !isPlayerA)) {
                // The player with advantage wins the game.
                game.setGameStatus(GameStatus.FINISHED);
            } else {
                // If the opponent wins, revert back to deuce.
                game.setGameStatus(GameStatus.DEUCE);
            }
        } else {
            // Normal score increment (not in deuce).
            if (isPlayerA) {
                game.setScorePlayerA(game.getScorePlayerA().next());
            } else {
                game.setScorePlayerB(game.getScorePlayerB().next());
            }

            // If after the update both players are at 40, then set the status to DEUCE.
            if (game.getScorePlayerA() == TennisPoint.FORTY && game.getScorePlayerB() == TennisPoint.FORTY) {
                game.setGameStatus(GameStatus.DEUCE);
            }
            // Otherwise, if one player is at 40 and the other is not, that player wins.
            else if (game.getScorePlayerA() == TennisPoint.FORTY && game.getScorePlayerB() != TennisPoint.FORTY) {
                game.setGameStatus(GameStatus.FINISHED);
            } else if (game.getScorePlayerB() == TennisPoint.FORTY && game.getScorePlayerA() != TennisPoint.FORTY) {
                game.setGameStatus(GameStatus.FINISHED);
            }
        }

        return tennisGameRepository.save(game);
    }
}
