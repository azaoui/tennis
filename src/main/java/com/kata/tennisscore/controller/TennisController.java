package com.kata.tennisscore.controller;


import com.kata.tennisscore.processor.TennisGameProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tennis")
public class TennisController {


    @Autowired
    private TennisGameProcessor processor;


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


        processor.processGameAsync(sequence);
        return ResponseEntity.ok("Game processing started with sequence: " + trimmedSequence);
    }

}


