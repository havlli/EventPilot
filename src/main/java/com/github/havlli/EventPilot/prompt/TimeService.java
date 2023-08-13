package com.github.havlli.EventPilot.prompt;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
public class TimeService {

    public Instant parseUtcInstant(String dateTime, String formatPattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatPattern);
        LocalDateTime localDateTime = LocalDateTime.parse(dateTime, formatter);
        return localDateTime.atZone(ZoneOffset.UTC).toInstant();
    }
}
