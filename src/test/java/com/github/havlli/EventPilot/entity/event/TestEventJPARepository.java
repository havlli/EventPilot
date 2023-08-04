package com.github.havlli.EventPilot.entity.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;

public interface TestEventJPARepository extends JpaRepository<Event, String> {

    @Query(value = "SELECT CURRENT_TIMESTAMP")
    Instant selectCurrentTimestamp();
}
