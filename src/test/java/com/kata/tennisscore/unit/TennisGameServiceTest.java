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
        when(tennisGameRepository.save(game)).thenReturn(game);
    }

    /**  Player A wins without DEUCE */
    @Test
    public void testSequence_AAAA_PlayerAWins() {
        tennisGameService.processBallSequence(game, "AAAA");
        assertEquals(GameStatus.FINISHED, game.getGameStatus());
        assertEquals(TennisPoint.FORTY, game.getScorePlayerA());
        assertNotEquals(TennisPoint.FORTY, game.getScorePlayerB());
    }

    @Test
    public void testSequence_AAA_GameInProgress() {
        tennisGameService.processBallSequence(game, "AAA");
        assertEquals(GameStatus.IN_PROGRESS, game.getGameStatus());
        assertEquals(TennisPoint.FORTY, game.getScorePlayerA());
        assertNotEquals(TennisPoint.FORTY, game.getScorePlayerB());
    }

    @Test
    public void testSequence_ABABABBB_PlayerBWins() {
        tennisGameService.processBallSequence(game, "ABABABBB");
        assertEquals(GameStatus.FINISHED, game.getGameStatus());

    }

    /**  Player B wins without DEUCE */
    @Test
    public void testSequence_BBBB_PlayerBWins() {
        tennisGameService.processBallSequence(game, "BBBB");
        assertEquals(GameStatus.FINISHED, game.getGameStatus());
        assertEquals(TennisPoint.FORTY, game.getScorePlayerB());
        assertNotEquals(TennisPoint.FORTY, game.getScorePlayerA());
    }

    /**  Game in Progress at 30-30 */
    @Test
    public void testSequence_ABAB_GameInProgress() {
        tennisGameService.processBallSequence(game, "ABAB");
        assertEquals(GameStatus.IN_PROGRESS, game.getGameStatus());
        assertEquals(TennisPoint.THIRTY, game.getScorePlayerA());
        assertEquals(TennisPoint.THIRTY, game.getScorePlayerB());
    }

    /**  Test correct handling of DEUCE and ADVANTAGE with Player A winning */
    @Test
    public void testSequence_ABABABABAA_PlayerAWinsAfterDeuce() {
        tennisGameService.processBallSequence(game, "ABABABABAA");
        assertEquals(GameStatus.FINISHED, game.getGameStatus());
        assertEquals(TennisPoint.FORTY, game.getScorePlayerA());
        assertEquals(TennisPoint.FORTY, game.getScorePlayerB());
    }

    /**  Test correct handling of DEUCE and ADVANTAGE with Player B winning */
    @Test
    public void testSequence_ABABABABB_PlayerBAVANCED() {
        tennisGameService.processBallSequence(game, "ABABABABB");
        assertEquals(GameStatus.ADVANTAGE_B, game.getGameStatus());
        assertEquals(TennisPoint.FORTY, game.getScorePlayerA());
        assertEquals(TennisPoint.FORTY, game.getScorePlayerB());
    }

    @Test
    public void testSequence_ABABABAA_PlayerAWIN() {
        tennisGameService.processBallSequence(game, "ABABABAA");
        assertEquals(GameStatus.FINISHED, game.getGameStatus());
        assertEquals(TennisPoint.FORTY, game.getScorePlayerA());
        assertEquals(TennisPoint.FORTY, game.getScorePlayerB());
    }

    /**  Ensure game does not continue after a player wins */
    @Test
    public void testGameStopsAfterWin() {
        tennisGameService.processBallSequence(game, "AAAA"); // Player A wins
        assertEquals(GameStatus.FINISHED, game.getGameStatus());

        tennisGameService.processBallEvent(game, "B");
        tennisGameService.processBallEvent(game, "A");
        assertEquals(GameStatus.FINISHED, game.getGameStatus());
    }

    /**  Invalid winner input should throw an exception */
    @Test
    public void testSequence_InvalidWinner_ThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            tennisGameService.processBallEvent(game, "C");
        });
        assertTrue(exception.getMessage().contains("Invalid ball winner"));
    }

    /**  Null input should throw an exception */
    @Test
    public void testSequence_NullWinner_ThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            tennisGameService.processBallEvent(game, null);
        });
        assertTrue(exception.getMessage().contains("Invalid ball winner"));
    }

    @Test
    public void testSequence_BABAABAB_GameContinues() {
        tennisGameService.processBallSequence(game, "BABAABAB");
        assertEquals(GameStatus.DEUCE, game.getGameStatus());
    }
}
