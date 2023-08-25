package com.github.havlli.EventPilot.generator;

import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class EmbedFormatter {

    public String raidSize(int current, int maximum) {
        return String.format("%s/%s", current, maximum);
    }

    public String leaderWithId(String leader, String id) {
        return String.format("Leader: %s - ID: %s", leader, id);
    }

    public String date(Instant dateTime) {
        return date(dateTime.getEpochSecond());
    }

    public String date(Long timestamp) {
        return String.format("<t:%d:D>", timestamp);
    }

    public String time(Instant dateTime) {
        return time(dateTime.getEpochSecond());
    }

    public String time(Long timestamp) {
        return String.format("<t:%d:t>", timestamp);
    }

    public String dateTime(Instant dateTime) {
        return dateTime(dateTime.getEpochSecond());
    }

    public String dateTime(Long timestamp) {
        return String.format("<t:%d:f>", timestamp);
    }

    public String relativeTime(Instant dateTime) {
        return relativeTime(dateTime.getEpochSecond());
    }

    public String relativeTime(Long timestamp) {
        return String.format("<t:%d:R>", timestamp);
    }
}
