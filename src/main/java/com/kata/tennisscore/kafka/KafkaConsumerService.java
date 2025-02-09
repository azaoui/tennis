package com.kata.tennisscore.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kata.tennisscore.domain.GameStatus;
import com.kata.tennisscore.domain.TennisGame;
import com.kata.tennisscore.dto.BallEventMessage;
import com.kata.tennisscore.repository.TennisGameRepository;
import com.kata.tennisscore.service.TennisGameService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
@Service
@Slf4j
public class KafkaConsumerService {


    @Autowired
    private TennisGameService tennisGameService;

    @Autowired
    private ObjectMapper objectMapper;

    private TennisGame currentGame;

    @Autowired
    private TennisGameRepository tennisGameRepository;

    @KafkaListener(topics = "${spring.kafka.topic.name}", groupId = "tennis-group")
    public void listen(@Payload ConsumerRecord<String, String> record, Acknowledgment acknowledgment)
            throws JsonProcessingException {
        try {
            BallEventMessage ballEventMessage = objectMapper.readValue(record.value(), BallEventMessage.class);
            TennisGame currentGame = tennisGameRepository.findById(ballEventMessage.getGameId()).orElse(null);

            // Check if currentGame is null
            if (currentGame == null) {
                log.error("Current game is null. Skipping processing.");
                acknowledgment.acknowledge(); // manual acknowledge
                return;
            }

            // If the game is already finished, acknowledge and skip processing.
            if (currentGame.getGameStatus() == GameStatus.FINISHED) {
                log.info("Game is already finished. Acknowledging message and skipping processing.");
                acknowledgment.acknowledge();  // manual acknowledge
                return;
            }

           tennisGameService.processBallEvent(currentGame, ballEventMessage.getWinner());

            // Manually acknowledge the offset only after successful processing.
            acknowledgment.acknowledge();  // manual acknowledge
        } catch (Exception e) {
            log.error("Error processing message", e);
            // Throwing the exception will force a rollback; the offset will not be committed.
            throw e;
        }
    }
}
