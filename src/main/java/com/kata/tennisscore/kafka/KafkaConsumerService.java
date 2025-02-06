package com.kata.tennisscore.kafka;

import com.kata.tennisscore.domain.GameStatus;
import com.kata.tennisscore.domain.TennisGame;
import com.kata.tennisscore.dto.BallEventMessage;
import com.kata.tennisscore.service.TennisGameService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class KafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    @Autowired
    private TennisGameService tennisGameService;

    @Autowired
    private ObjectMapper objectMapper;

    private TennisGame currentGame;

    public void setCurrentGame(TennisGame game) {
        this.currentGame = game;
    }

    @KafkaListener(topics = "${spring.kafka.topic.name}", groupId = "tennis-group")
    public void listen(@Payload ConsumerRecord<String, String> record, Acknowledgment acknowledgment)
            throws JsonProcessingException {
        try {
            BallEventMessage ballEventMessage = objectMapper.readValue(record.value(), BallEventMessage.class);
            String ballWinner = ballEventMessage.getWinner().equalsIgnoreCase("A") ? "Player A" : "Player B";

            // Check if currentGame is null
            if (currentGame == null) {
                logger.error("Current game is null. Skipping processing.");
                acknowledgment.acknowledge(); // manual acknowledge
                return;
            }

            // If the game is already finished, acknowledge and skip processing.
            if (currentGame.getGameStatus() == GameStatus.FINISHED) {
                logger.info("Game is already finished. Acknowledging message and skipping processing.");
                acknowledgment.acknowledge();  // manual acknowledge
                return;
            }

            // Process the ball event.
            currentGame = tennisGameService.processBallEvent(currentGame, ballEventMessage.getWinner());

            // Manually acknowledge the offset only after successful processing.
            acknowledgment.acknowledge();  // manual acknowledge
        } catch (Exception e) {
            logger.error("Error processing message", e);
            // Throwing the exception will force a rollback; the offset will not be committed.
            throw e;
        }
    }
}
