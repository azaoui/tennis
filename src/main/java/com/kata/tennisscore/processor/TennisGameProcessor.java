package com.kata.tennisscore.processor;

import com.kata.tennisscore.domain.Player;
import com.kata.tennisscore.domain.TennisGame;
import com.kata.tennisscore.dto.BallEventMessage;
import com.kata.tennisscore.kafka.KafkaConsumerService;
import com.kata.tennisscore.kafka.KafkaProducerService;
import com.kata.tennisscore.repository.TennisGameRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TennisGameProcessor {


    @Autowired
    private TennisGameRepository tennisGameRepository;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Async
    public void processGameAsync(String sequence) {
        TennisGame game = new TennisGame(new Player("Player A"), new Player("Player B"));
        game.setBallSequence(sequence);
        game = tennisGameRepository.save(game);
        log.info("Starting async processing for Game ID: {}", game.getGameId());
        int ballNumber = 1;
        for (char c : sequence.toCharArray()) {
            log.debug("Processing ball #{} - Winner: {}", ballNumber, c);
            BallEventMessage message = new BallEventMessage(
                    game.getGameId(),
                    ballNumber,
                    String.valueOf(c)
            );
            kafkaProducerService.sendBallEvent(message);
            ballNumber++;

            try {
                Thread.sleep(1000); // Simulating event delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

