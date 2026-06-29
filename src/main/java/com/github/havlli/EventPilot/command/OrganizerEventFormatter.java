package com.github.havlli.EventPilot.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.havlli.EventPilot.entity.embedtype.EmbedTypeService;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.participant.Participant;
import com.github.havlli.EventPilot.entity.participant.ParticipantStatus;
import com.github.havlli.EventPilot.generator.EmbedFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OrganizerEventFormatter {

    private static final Logger LOG = LoggerFactory.getLogger(OrganizerEventFormatter.class);
    private static final int DISCORD_CONTENT_LIMIT = 1900;
    private static final String TRUNCATION_SUFFIX = "\n... truncated";
    private final EmbedFormatter embedFormatter;
    private final EmbedTypeService embedTypeService;

    public OrganizerEventFormatter(EmbedFormatter embedFormatter, EmbedTypeService embedTypeService) {
        this.embedFormatter = embedFormatter;
        this.embedTypeService = embedTypeService;
    }

    public String formatEventList(List<Event> events) {
        String content = events.stream()
                .map(this::formatEventListEntry)
                .collect(Collectors.joining("\n"));

        return truncate("Events:\n" + content);
    }

    public String formatEventDetails(Event event) {
        Map<Integer, String> roleNames = roleNames(event);
        String roles = formatRoleGroups(event, roleNames);
        String waitlist = formatWaitlist(event, roleNames);

        String content = """
                Event: **%s**
                ID: `%s`
                Status: %s
                Starts: %s (%s)
                Channel: %s
                Leader: %s
                Roster: %s

                Confirmed roles:
                %s

                Waitlist:
                %s
                """.formatted(
                event.getName(),
                event.getEventId(),
                embedFormatter.status(event.getStatus()),
                embedFormatter.dateTime(event.getDateTime()),
                embedFormatter.relativeTime(event.getDateTime()),
                formatChannel(event),
                event.getAuthor(),
                formatRoster(event),
                roles,
                waitlist
        );

        return truncate(content.strip());
    }

    private String formatEventListEntry(Event event) {
        return "- **%s** | %s | %s | %s | %s | ID: `%s`".formatted(
                event.getName(),
                embedFormatter.status(event.getStatus()),
                embedFormatter.dateTime(event.getDateTime()),
                formatChannel(event),
                formatRoster(event),
                event.getEventId()
        );
    }

    private String formatRoster(Event event) {
        return "%d/%s confirmed, %d waitlisted".formatted(
                confirmedCapacityCount(event),
                event.getMemberSize(),
                waitlistedCount(event)
        );
    }

    private String formatRoleGroups(Event event, Map<Integer, String> roleNames) {
        Map<Integer, List<Participant>> participantsByRole = event.getParticipants()
                .stream()
                .filter(this::isSignedUp)
                .collect(Collectors.groupingBy(Participant::getRoleIndex));

        if (participantsByRole.isEmpty()) {
            return "No confirmed participants.";
        }

        return participantsByRole.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey(this::compareRoleIndexes))
                .map(entry -> "%s (%d): %s".formatted(
                        roleName(entry.getKey(), roleNames),
                        entry.getValue().size(),
                        formatParticipants(entry.getValue())
                ))
                .collect(Collectors.joining("\n"));
    }

    private String formatParticipants(List<Participant> participants) {
        return participants.stream()
                .sorted(Comparator.comparing(Participant::getPosition))
                .map(participant -> "`%d` %s".formatted(participant.getPosition(), participant.getUsername()))
                .collect(Collectors.joining(", "));
    }

    private String formatWaitlist(Event event, Map<Integer, String> roleNames) {
        String waitlisted = event.getParticipants()
                .stream()
                .filter(this::isWaitlisted)
                .sorted(Comparator.comparing(Participant::getPosition))
                .map(participant -> "`%d` %s - %s".formatted(
                        participant.getPosition(),
                        participant.getUsername(),
                        roleName(participant.getRoleIndex(), roleNames)
                ))
                .collect(Collectors.joining("\n"));

        return waitlisted.isBlank() ? "No waitlisted participants." : waitlisted;
    }

    private Map<Integer, String> roleNames(Event event) {
        try {
            return embedTypeService.getDeserializedMap(event.getEmbedType());
        } catch (JsonProcessingException error) {
            LOG.warn("Could not deserialize role names for event [{}]", event.getEventId(), error);
            return Map.of();
        }
    }

    private String roleName(Integer roleIndex, Map<Integer, String> roleNames) {
        return roleNames.getOrDefault(roleIndex, "Role " + roleIndex);
    }

    private String formatChannel(Event event) {
        if (event.getDestinationChannelId() == null || event.getDestinationChannelId().isBlank()) {
            return "unknown channel";
        }

        return "<#%s>".formatted(event.getDestinationChannelId());
    }

    private int confirmedCapacityCount(Event event) {
        return (int) event.getParticipants()
                .stream()
                .filter(participant -> isSignedUp(participant)
                        && participant.getRoleIndex() != null
                        && participant.getRoleIndex() > 0)
                .count();
    }

    private int waitlistedCount(Event event) {
        return (int) event.getParticipants()
                .stream()
                .filter(this::isWaitlisted)
                .count();
    }

    private boolean isSignedUp(Participant participant) {
        return participant.getStatus() == ParticipantStatus.SIGNED_UP;
    }

    private boolean isWaitlisted(Participant participant) {
        return participant.getStatus() == ParticipantStatus.WAITLISTED;
    }

    private int compareRoleIndexes(Integer first, Integer second) {
        if (first > 0 && second > 0) {
            return Integer.compare(first, second);
        }
        if (first <= 0 && second <= 0) {
            return Integer.compare(first, second);
        }
        return first > 0 ? -1 : 1;
    }

    private String truncate(String content) {
        if (content.length() <= DISCORD_CONTENT_LIMIT) {
            return content;
        }

        return content.substring(0, DISCORD_CONTENT_LIMIT - TRUNCATION_SUFFIX.length()) + TRUNCATION_SUFFIX;
    }
}
