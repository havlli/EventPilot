package com.github.havlli.EventPilot.command;

import discord4j.core.event.domain.Event;
import reactor.core.publisher.Mono;

public interface SlashCommand {
    /**
     * Returns the name of the event.
     *
     * @return The name of the event.
     */
    String getName();
    /**
     * Get the event type of this class.
     *
     * @return The event type.
     */
    Class<? extends Event> getEventType();
    /**
     * Set the event type for this object.
     *
     * @param eventType The event type to set.
     */
    void setEventType(Class<? extends Event> eventType);
    /**
     * Handles the given event.
     *
     * @param event the event to handle
     * @return a Mono representing the result of handling the event
     */
    Mono<?> handle(Event event);
}
