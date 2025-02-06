package com.kata.tennisscore.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "players")
public class Player {

    @Id
    @Column(name = "player_id", nullable = false)
    private UUID id;

    @Column(name = "name")
    private String name;

    public Player() {
        this.id = UUID.randomUUID();
    }

    public Player(String name) {
        this.id = UUID.randomUUID();
        this.name = name;
    }

    // Getters and setters.
    public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
}

