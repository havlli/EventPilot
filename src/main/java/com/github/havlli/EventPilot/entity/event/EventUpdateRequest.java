package com.github.havlli.EventPilot.entity.event;

public record EventUpdateRequest(
        String name,
        String description,
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
