package com.kata.tennisscore.unit;

import com.kata.tennisscore.domain.GameStatus;
import com.kata.tennisscore.domain.Player;
import com.kata.tennisscore.domain.TennisGame;
import com.kata.tennisscore.domain.TennisPoint;
import com.kata.tennisscore.repository.TennisGameRepository;
import com.kata.tennisscore.service.TennisGameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class TennisGameServiceTest {

    @Mock
    private TennisGameRepository tennisGameRepository;

    @InjectMocks
    private TennisGameService tennisGameService;

    private TennisGame game;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        game = new TennisGame(new Player("Player A"), new Player("Player B"));
        // When save is called, simply return the same game instance.
        when(tennisGameRepository.save(game)).thenReturn(game);
    }

    @Test
    public void testSequence_AAAA_PlayerAWins() {
        // "AAAA" -> Player A wins.
        game = tennisGameService.processBallEvent(game, "A"); // 15-0
        game = tennisGameService.processBallEvent(game, "A"); // 30-0
        game = tennisGameService.processBallEvent(game, "A"); // 40-0, game finished
        assertEquals(GameStatus.FINISHED, game.getGameStatus());
        assertEquals(TennisPoint.FORTY, game.getScorePlayerA());
        assertNotEquals(TennisPoint.FORTY, game.getScorePlayerB());
    }

    @Test
    public void testSequence_BBBB_PlayerBWins() {
        // "BBBB" -> Player B wins.
        game = tennisGameService.processBallEvent(game, "B"); // 15-0 for B
        game = tennisGameService.processBallEvent(game, "B"); // 30-0 for B
        game = tennisGameService.processBallEvent(game, "B"); // 40-0 for B, game finished
        assertEquals(GameStatus.FINISHED, game.getGameStatus());
        assertEquals(TennisPoint.FORTY, game.getScorePlayerB());
        assertNotEquals(TennisPoint.FORTY, game.getScorePlayerA());
    }

    @Test
    public void testSequence_ABAB_GameInProgress() {
        // "ABAB" -> Game should be in progress (30-30).
        game = tennisGameService.processBallEvent(game, "A"); // 15-0
        game = tennisGameService.processBallEvent(game, "B"); // 15-15
        game = tennisGameService.processBallEvent(game, "A"); // 30-15
        game = tennisGameService.processBallEvent(game, "B"); // 30-30
        assertEquals(GameStatus.IN_PROGRESS, game.getGameStatus());
        assertEquals(TennisPoint.THIRTY, game.getScorePlayerA());
        assertEquals(TennisPoint.THIRTY, game.getScorePlayerB());
    }

    @Test
    public void testSequence_ABABAA_PlayerAWins() {
        // "ABABAA" -> Expected: Player A wins the game (finishes at 40-30).
        game = tennisGameService.processBallEvent(game, "A"); // 15-0
        game = tennisGameService.processBallEvent(game, "B"); // 15-15
        game = tennisGameService.processBallEvent(game, "A"); // 30-15
        game = tennisGameService.processBallEvent(game, "B"); // 30-30
        game = tennisGameService.processBallEvent(game, "A"); // 40-30, game finished
        assertEquals(GameStatus.FINISHED, game.getGameStatus());
        assertEquals(TennisPoint.FORTY, game.getScorePlayerA());
        assertNotEquals(TennisPoint.FORTY, game.getScorePlayerB());
    }

    @Test
    public void testSequence_InvalidWinner_ThrowsException() {
        // Test that passing an invalid winner (e.g., "C") throws an exception.
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            tennisGameService.processBallEvent(game, "C");
        });
        assertTrue(exception.getMessage().contains("Invalid ball winner"));
    }

    @Test
    public void testSequence_NullWinner_ThrowsException() {
        // Test that passing a null winner throws an exception.
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            tennisGameService.processBallEvent(game, null);
        });
        assertTrue(exception.getMessage().contains("Invalid ball winner"));
    }

    @Test
    public void testSequence_DeuceAndAdvantage_PlayerBWins() {
        // This test forces a deuce scenario and then ensures Player B wins.
        // First, manually set the game score to 30-30.
        game.setScorePlayerA(TennisPoint.THIRTY);
        game.setScorePlayerB(TennisPoint.THIRTY);
        game.setGameStatus(GameStatus.IN_PROGRESS);

        // Ball: B wins -> moves from 30 to 40.
        game = tennisGameService.processBallEvent(game, "B");
        // Force the score to 40-40 to simulate deuce.
        game.setScorePlayerA(TennisPoint.FORTY);
        game.setScorePlayerB(TennisPoint.FORTY);
        game.setGameStatus(GameStatus.DEUCE);

        // Next ball: B wins -> moves to ADVANTAGE_B.
        game = tennisGameService.processBallEvent(game, "B");
        // Next ball: B wins -> game finishes.
        game = tennisGameService.processBallEvent(game, "B");

        // Verify game is finished.
        assertEquals(GameStatus.FINISHED, game.getGameStatus());
    }
}

