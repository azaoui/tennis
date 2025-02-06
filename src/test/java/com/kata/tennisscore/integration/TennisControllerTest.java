package com.kata.tennisscore.integration;


import com.kata.tennisscore.controller.TennisController;
import com.kata.tennisscore.domain.Player;
import com.kata.tennisscore.domain.TennisGame;
import com.kata.tennisscore.kafka.KafkaConsumerService;
import com.kata.tennisscore.kafka.KafkaProducerService;
import com.kata.tennisscore.repository.TennisGameRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TennisController.class)
public class TennisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KafkaProducerService kafkaProducerService;

    @MockBean
    private KafkaConsumerService kafkaConsumerService;

    @MockBean
    private TennisGameRepository tennisGameRepository;

    @Test
    public void testValidSequence() throws Exception {
        // Create a dummy game object.
        TennisGame game = new TennisGame(new Player("Player A"), new Player("Player B"));
        // (The constructor automatically generates a gameId)
        UUID gameId = game.getGameId();

        // When saving, simply return the game.
        when(tennisGameRepository.save(ArgumentMatchers.any(TennisGame.class))).thenReturn(game);

        // Send a valid sequence "ABABAA"
        mockMvc.perform(post("/api/tennis/play")
                        .content("ABABAA")
                        .contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Game processing started with sequence: ABABAA")));
    }

    @Test
    public void testEmptySequence() throws Exception {
        // Expect a 400 error when sending an empty sequence.
        mockMvc.perform(post("/api/tennis/play")
                        .content("")
                        .contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Error: Sequence payload cannot be empty.")));
    }

    @Test
    public void testInvalidSequence() throws Exception {
        // Expect a 400 error when the sequence contains invalid characters (only 'A' and 'B' allowed).
        mockMvc.perform(post("/api/tennis/play")
                        .content("ABAC")
                        .contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Error: Invalid sequence payload. Only characters 'A' and 'B' are allowed.")));
    }
}
