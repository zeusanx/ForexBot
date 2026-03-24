package com.forexbot.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BotStateRepository extends JpaRepository<BotState, Long> {
    Optional<BotState> findByStateKey(String stateKey);
}
