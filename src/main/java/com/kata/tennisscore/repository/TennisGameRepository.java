package com.kata.tennisscore.repository;


import com.kata.tennisscore.domain.TennisGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TennisGameRepository extends JpaRepository<TennisGame, UUID> {
}

