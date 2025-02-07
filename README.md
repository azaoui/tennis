# tennis scoring kafka demo
Event driven Tennis Scoring System with Kafka and Spring Boot


       [Client HTTP Request]
                  │
                  │ POST /api/tennis/play with payload "ABABAA"
                  ▼
       ────────────────────────────────────────
       │   TennisController                   │
       │ (REST Endpoint)                      │
       │  - Validates payload                 │
       │  - Creates a new game                │
       │  - Persists game (DB)                │
       │  - Sets game in consumer service     │
       │  - Iterates over "ABABAA"            │
       ────────────────────────────────────────
                  │
       For each character (ball):
                  │
                  ▼
       ┌───────────────────────────────-┐
       │  Create BallEventMessage       │
       │  (gameId, ballNumber, winner,  │
       │   timestamp)                   │
       └────────────────────────────────┘
                  │
                  │  KafkaProducerService.sendBallEvent(message)
                  ▼
       ┌────────────────────────┐
       │  Kafka Producer TX     │
       │  - Executes transaction│
       │  - Publishes message   │
       └────────────────────────┘
                  │
                  ▼
       ┌────────────────────────┐
       │   Kafka Broker/Topic   │
       │  (tennis-score)        │
       │  - Message stored      │
       └────────────────────────┘
                  │
                  ▼
       ┌────────────────────────┐
       │  Kafka Consumer        │
       │  (KafkaConsumerService)│
       │  - Listens to topic    │
       │  - Receives committed  │
       │    messages            │
       │  - Deserializes message│
       └────────────────────────┘
                  │
                  ▼
       ┌────────────────────────┐
       │ TennisGameService      │
       │  - Updates game state  │
       │  - Applies tennis rules│
       └────────────────────────┘
                  │
                  ▼
       ┌────────────────────────┐
       │  DB (PostgreSQL)       │
       │  - Game state updated  │
       └────────────────────────┘
                  │
                  ▼
       ┌────────────────────────┐
       │ KafkaConsumerService   │
       │  - Acknowledges message│
       │  - Logs current score  │
       │    (or final result)   │
       └────────────────────────┘
                  │
                  ▼
       [Console Output / Logs]



## Scalability & Parallel Processing

We create a topic with 3 partition and replicat factor set to 2 ( for this demo we have 3 broker so the choise was impact with this constrainte)

- Kafka distributes messages across partitions, allowing multiple consumers to read in parallel (within the same consumer group).
- More partitions → Higher throughput.
- Helps scale out processing across multiple consumers

```properties
spring.kafka.topic.name=tennis-score
spring.kafka.topic.partitions=3
spring.kafka.topic.replication-factor=2
```

```java
    @Bean
    public NewTopic createTopic() {
        return new NewTopic(topicName, partitions, replicationFactor);
    }
```


### How the order is maintanning so we can use kafka with multi patition topic:

In our tennis scoring system, we use gameId as the key to ensure that all events for the same game (ball events) go to the same partition in Kafka. This guarantees that the events are processed in the correct order, preventing scoring inconsistencies across partitions.
```java
kafkaTemplate.executeInTransaction(operation -> {
                ProducerRecord<String, String> record = new ProducerRecord<>(
                        topicName,
                        message.getGameId().toString(),  // Key: gameId is the partition id to insure the correct order
                        jsonMessage  // Value: Serialized JSON message
                );

```


## exactly-once semantics (EOS)


To achive exactly-once semantics in the demo tennis kafka we use this config

**At Producer side :**

```properties
spring.kafka.producer.transaction-id-prefix=tennis-tx-
spring.kafka.producer.enable-idempotence=true
spring.kafka.producer.acks=all
```


 - The idempotence  is enabled to prevent message duplication even in failure case
 - **spring.kafka.producer.transaction-id-prefix** to enable transaction (see https://docs.spring.io/spring-kafka/reference/kafka/transactions.html)

The producer must also explicitly manage transactions we use in this example the kafka template methode **executeInTransaction**

```java
  kafkaTemplate.executeInTransaction(operation -> {
                ProducerRecord<String, String> record = new ProducerRecord<>(
                        topicName,
                        message.getGameId().toString(), 
                        jsonMessage  // Value: Serialized JSON message
                );
```

**At Consumer side :**

The consumer should be configured with isolation.level=read_committed to ensure it only reads committed messages (messages that are part of a successfully committed transaction).

```properties
spring.kafka.consumer.isolation-level=READ_COMMITTED
```

**Disable Auto-Commit**


If auto-commit is enabled, the consumer might commit offsets before the messages are fully processed. If the consumer crashes or fails after committing the offsets but before processing the messages, those messages will be lost because the consumer will not re-read them. So it importent to disable auto commit

```properties
spring.kafka.consumer.enable-auto-commit=false
```

**Enable MANUAL AckMode** 


```java 
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
```
**Manual Commit offset** 



```java
            if (currentGame.getGameStatus() == GameStatus.FINISHED) {
                logger.info("Game is already finished. Acknowledging message and skipping processing.");
                acknowledgment.acknowledge();  // manual acknowledge
                return;
            }
```



### Docker-compose used for this demo

```yaml
version: '2.1'

services:
  # ------------------------
  # Zookeeper Cluster (3 Nodes)
  # ------------------------
  zoo1:
    image: confluentinc/cp-zookeeper:7.3.2
    hostname: zoo1
    container_name: zoo1
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_SERVER_ID: 1
      ZOOKEEPER_SERVERS: zoo1:2888:3888;zoo2:2888:3888;zoo3:2888:3888

  zoo2:
    image: confluentinc/cp-zookeeper:7.3.2
    hostname: zoo2
    container_name: zoo2
    ports:
      - "2182:2182"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2182
      ZOOKEEPER_SERVER_ID: 2
      ZOOKEEPER_SERVERS: zoo1:2888:3888;zoo2:2888:3888;zoo3:2888:3888

  zoo3:
    image: confluentinc/cp-zookeeper:7.3.2
    hostname: zoo3
    container_name: zoo3
    ports:
      - "2183:2183"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2183
      ZOOKEEPER_SERVER_ID: 3
      ZOOKEEPER_SERVERS: zoo1:2888:3888;zoo2:2888:3888;zoo3:2888:3888

  # ------------------------
  # Kafka Brokers (3 Nodes)
  # ------------------------
  kafka1:
    image: confluentinc/cp-kafka:7.3.2
    hostname: kafka1
    container_name: kafka1
    ports:
      - "9092:9092"
      - "29092:29092"
    environment:
      KAFKA_ADVERTISED_LISTENERS: INTERNAL://kafka1:19092,EXTERNAL://${DOCKER_HOST_IP:-127.0.0.1}:9092,DOCKER://host.docker.internal:29092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: INTERNAL:PLAINTEXT,EXTERNAL:PLAINTEXT,DOCKER:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL
      KAFKA_ZOOKEEPER_CONNECT: "zoo1:2181,zoo2:2182,zoo3:2183"
      KAFKA_BROKER_ID: 1

    depends_on:
      - zoo1
      - zoo2
      - zoo3

  kafka2:
    image: confluentinc/cp-kafka:7.3.2
    hostname: kafka2
    container_name: kafka2
    ports:
      - "9093:9093"
      - "29093:29093"
    environment:
      KAFKA_ADVERTISED_LISTENERS: INTERNAL://kafka2:19093,EXTERNAL://${DOCKER_HOST_IP:-127.0.0.1}:9093,DOCKER://host.docker.internal:29093
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: INTERNAL:PLAINTEXT,EXTERNAL:PLAINTEXT,DOCKER:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL
      KAFKA_ZOOKEEPER_CONNECT: "zoo1:2181,zoo2:2182,zoo3:2183"
      KAFKA_BROKER_ID: 2

    depends_on:
      - zoo1
      - zoo2
      - zoo3

  kafka3:
    image: confluentinc/cp-kafka:7.3.2
    hostname: kafka3
    container_name: kafka3
    ports:
      - "9094:9094"
      - "29094:29094"
    environment:
      KAFKA_ADVERTISED_LISTENERS: INTERNAL://kafka3:19094,EXTERNAL://${DOCKER_HOST_IP:-127.0.0.1}:9094,DOCKER://host.docker.internal:29094
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: INTERNAL:PLAINTEXT,EXTERNAL:PLAINTEXT,DOCKER:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL
      KAFKA_ZOOKEEPER_CONNECT: "zoo1:2181,zoo2:2182,zoo3:2183"
      KAFKA_BROKER_ID: 3

    depends_on:
      - zoo1
      - zoo2
      - zoo3

  # ------------------------
  # Kafka UI
  # ------------------------
  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: kafka-ui
    ports:
      - "8090:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: "local-cluster"
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: "kafka1:19092,kafka2:19093,kafka3:19094"
      KAFKA_CLUSTERS_0_ZOOKEEPER: "zoo1:2181,zoo2:2181,zoo3:2181"
    depends_on:
      - kafka1
      - kafka2
      - kafka3


  # ------------------------
  # PostgreSQL Database
  # ------------------------
  postgres:
    image: postgres:15
    container_name: postgres
    restart: always
    environment:
      POSTGRES_DB: tennisdb
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: admin123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  # ------------------------
  # pgAdmin (Optional for DB Management)
  # ------------------------
  pgadmin:
    image: dpage/pgadmin4
    container_name: pgadmin
    restart: always
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@admin.com
      PGADMIN_DEFAULT_PASSWORD: admin
    ports:
      - "5050:80"
    depends_on:
      - postgres

volumes:
  postgres_data:
    driver: local


```





**This Docker Compose configuration sets up a Kafka cluster (3 brokers) with ZooKeeper (3 nodes), a PostgreSQL database, and Kafka UI for monitoring. It includes pgAdmin for managing the PostgreSQL database. The setup enables high availability and scalability for Kafka messaging and database persistence.**


### Demo : 


Clone the project and run 
```
run docker-compose up -d // to run brokers
```
```
mvn package // to package the jar
```

```
java -jar target\tennis-kafka-0.0.1-SNAPSHOT.jar to start the app
```

```
curl -X POST "http://localhost:8080/api/tennis/play" \
     -H "Content-Type: text/plain" \
     -d "ABABAA"

```
![image](https://github.com/user-attachments/assets/4e0ca7ea-66c3-4f80-8d3c-f0bec5fc7e8a)

![image](https://github.com/user-attachments/assets/082972ee-783f-4992-814a-042b674d6cd2)


![image](https://github.com/user-attachments/assets/c9932dd4-2913-49ac-8952-c7971faf095d)







