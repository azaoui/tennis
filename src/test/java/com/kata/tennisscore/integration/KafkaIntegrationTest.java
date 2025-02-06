package com.kata.tennisscore.integration;


import com.kata.tennisscore.dto.BallEventMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;


import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(partitions = 3, topics = { "tennis-score" })
public class KafkaIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private ConsumerFactory<String, String> consumerFactory;

    @BeforeEach
    public void setUp() {
        // Set up consumer properties for the embedded Kafka broker.
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafkaBroker);
        consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer());
    }

    @Test
    public void testSendAndReceiveBallEventMessage() throws Exception {
        // Create a sample BallEventMessage
        BallEventMessage message = new BallEventMessage();
        message.setGameId(java.util.UUID.randomUUID());
        message.setBallNumber(1);
        message.setWinner("A");

        // Serialize the message to JSON
        String jsonMessage = objectMapper.writeValueAsString(message);

        // Set up a CountDownLatch for synchronization (we expect one message)
        CountDownLatch latch = new CountDownLatch(1);

        // Create container properties to listen to the topic
        ContainerProperties containerProperties = new ContainerProperties("tennis-score");
        ConcurrentMessageListenerContainer<String, String> container =
                new ConcurrentMessageListenerContainer<>(consumerFactory, containerProperties);

        // Set a message listener to process incoming messages.
        container.setupMessageListener((org.springframework.kafka.listener.MessageListener<String, String>) record -> {
            try {
                BallEventMessage receivedMessage = objectMapper.readValue(record.value(), BallEventMessage.class);
                // Perform assertions on the received message.
                assertThat(receivedMessage.getWinner()).isEqualTo("A");
                latch.countDown(); // Signal that the message was received.
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        container.start();

        // Wait until the container is assigned partitions.
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());

        // Send the message using the KafkaTemplate.

// Instead of calling send() directly, wrap the call in a transaction.
        kafkaTemplate.executeInTransaction(ops -> {
            ops.send("tennis-score", message.getGameId().toString(), jsonMessage);
            return true; // Indicate success so the transaction commits.
        });

        // Wait for the message to be received.
        boolean messageReceived = latch.await(10, TimeUnit.SECONDS);
        container.stop();

        // Assert that the message was received.
        assertThat(messageReceived).isTrue();
    }
}

