package com.kata.tennisscore.controller;

import com.kata.tennisscore.domain.Player;
import com.kata.tennisscore.domain.TennisGame;
import com.kata.tennisscore.dto.BallEventMessage;
import com.kata.tennisscore.kafka.KafkaConsumerService;
import com.kata.tennisscore.kafka.KafkaProducerService;
import com.kata.tennisscore.repository.TennisGameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tennis")
public class TennisController {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private KafkaConsumerService kafkaConsumerService;

    @Autowired
    private TennisGameRepository tennisGameRepository;

    /**
     * Starts a new game by processing a sequence of ball events (e.g., "ABABAA").
     * Each ball event is sent as a detailed JSON message via Kafka.
     */
    @PostMapping("/play")
    public ResponseEntity<String> playGame(@RequestBody(required = false) String sequence) {
        // Validate payload immediately
        if (sequence == null || sequence.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Error: Sequence payload cannot be empty.");
        }

        String trimmedSequence = sequence.trim().toUpperCase();

        // Validate that the sequence contains only 'A' or 'B'
        if (!trimmedSequence.matches("[AB]+")) {
            return ResponseEntity.badRequest().body("Error: Invalid sequence payload. Only characters 'A' and 'B' are allowed.");
        }

        // Create a new game with two players.
        TennisGame game = new TennisGame(new Player("Player A"), new Player("Player B"));
        game.setBallSequence(trimmedSequence);
        game = tennisGameRepository.save(game);
        kafkaConsumerService.setCurrentGame(game);

        int ballNumber = 1;
        for (char c : trimmedSequence.toCharArray()) {
            BallEventMessage message = new BallEventMessage(
                    game.getGameId(),
                    ballNumber,
                    String.valueOf(c) // the winner will be "A" or "B"

            );
            kafkaProducerService.sendBallEvent(message);
            ballNumber++;
            // Simulate a delay to allow processing before sending the next event.
            try {
                Thread.sleep(1000); // 1-second delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return ResponseEntity.ok("Game processing started with sequence: " + trimmedSequence);
    }
}
