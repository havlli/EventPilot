package com.github.havlli.EventPilot.command;

import discord4j.core.event.domain.lifecycle.ReadyEvent;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class EventTypeComparator implements Comparator<SlashCommand> {
    @Override
    public int compare(SlashCommand command1, SlashCommand command2) {
        Class<?> eventType1 = command1.getEventType();
        Class<?> eventType2 = command2.getEventType();

        boolean isOnReadyEvent1 = eventType1.equals(ReadyEvent.class);
        boolean isOnReadyEvent2 = eventType2.equals(ReadyEvent.class);

        if (isOnReadyEvent1 && isOnReadyEvent2) {
            return 0;
        } else if (isOnReadyEvent1) {
            return -1;
        } else if (isOnReadyEvent2) {
            return 1;
        } else {
            return eventType1.getCanonicalName().compareTo(eventType2.getCanonicalName());
        }
    }
}
