package com.github.havlli.EventPilot.generator;

import com.github.havlli.EventPilot.entity.participant.Participant;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EmbedFormatter {

    public String raidSize(int current, int maximum) {
        return String.format("%d/%d", current, maximum);
    }

    public String raidSize(int current, String maximum) {
        return String.format("%d/%s", current, maximum);
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

    public String createConcatField(String fieldName, List<Participant> matchingUsers, boolean isOneLineField) {
        String lineSeparator = isOneLineField ? ", " : "\n";
        String lineBreak = isOneLineField ? " " : "\n";
        String concatUsers = matchingUsers.stream()
                .map(participant -> String.format("`%d`%s", participant.getPosition(), participant.getUsername()))
                .collect(Collectors.joining(lineSeparator));

        return String.format("%s (%d):%s%s",
                fieldName,
                matchingUsers.size(),
                lineBreak,
                concatUsers
        );
    }
}
