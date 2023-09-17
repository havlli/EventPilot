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
        LocalDateTime localDateTime = parseDateTime(dateTime, formatPattern);
        return utcDateTimeToInstant(localDateTime);
    }

    public boolean isValidFutureTime(Instant testedTime) {
        Instant currentTime = getCurrentTime();
        if (!currentTime.isBefore(testedTime)) {
            throwInvalidDateTimeException(testedTime, currentTime);
        }
        return true;
    }

    private DateTimeFormatter getDateTimeFormatter(String pattern) {
        return DateTimeFormatter.ofPattern(pattern);
    }

    private LocalDateTime parseDateTime(String dateTime, String pattern) {
        return LocalDateTime.parse(dateTime, getDateTimeFormatter(pattern));
    }

    private Instant utcDateTimeToInstant(LocalDateTime localDateTime) {
        return localDateTime.toInstant(ZoneOffset.UTC);
    }

    private Instant getCurrentTime() {
        return Instant.now();
    }

    private void throwInvalidDateTimeException(Instant testedTime, Instant currentTime) {
        throw new InvalidDateTimeException("Processed instant {%s} is before System Instant {%s}"
                .formatted(testedTime, currentTime));
    }
}
