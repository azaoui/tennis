package com.kata.tennisscore.kafka;


import com.kata.tennisscore.dto.BallEventMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;


@Service
public class KafkaProducerService {

    @Value("${spring.kafka.topic.name}")
    private String topicName;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public void sendBallEvent(BallEventMessage message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);

            kafkaTemplate.executeInTransaction(operation -> {
                ProducerRecord<String, String> record = new ProducerRecord<>(
                        topicName,
                        message.getGameId().toString(),  // Key: gameId is the partition id to insure the correct order
                        jsonMessage  // Value: Serialized JSON message
                );

                // Add timestamp header to Kafka message
                record.headers().add(new RecordHeader("timestamp", String.valueOf(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8)));

                // Send message inside transaction
                operation.send(record);

                return true; // Ensures the transaction commits successfully
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

