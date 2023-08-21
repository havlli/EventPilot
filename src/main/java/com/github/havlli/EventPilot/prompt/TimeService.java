package com.github.havlli.EventPilot.prompt;

import com.github.havlli.EventPilot.exception.InvalidDateTimeException;
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

    public boolean isValidFutureTime(Instant instant) {
        Instant instantNow = Instant.now();
        if (!instantNow.isBefore(instant)) {
            throw new InvalidDateTimeException("Processed instant {%s} is before System Instant.now() {%s}"
                    .formatted(instant, instantNow));
        }
        return true;
    }
}
