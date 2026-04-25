package com.github.havlli.EventPilot.entity.event;

import jakarta.validation.constraints.Size;

public record EventUpdateRequest(
        @Size(min = 1, max = 50)
        String name,
        String description,
        @Size(min = 1, max = 5)
        String memberSize
) {
    public Event updateEvent(Event event) {
        Event.Builder builder = Event.builder().fromEvent(event);

        if (name != null) builder.withName(name);
        if (description != null) builder.withDescription(description);
        if (memberSize != null) builder.withMemberSize(memberSize);

        return builder.build();
    }
}
