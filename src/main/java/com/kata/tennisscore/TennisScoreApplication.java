package com.kata.tennisscore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TennisScoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(TennisScoreApplication.class, args);
    }
}
