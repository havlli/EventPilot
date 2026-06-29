package com.github.havlli.EventPilot.entity.event;

public record EventSignupResult(Outcome outcome, Event event) {

    public enum Outcome {
        ADDED,
        UPDATED,
        WAITLISTED,
        EVENT_FULL,
        EVENT_NOT_FOUND,
        ROLE_NOT_FOUND,
        INVALID_CAPACITY,
        EVENT_CLOSED,
        EVENT_CANCELLED,
        EVENT_EXPIRED
    }

    public static EventSignupResult added(Event event) {
        return new EventSignupResult(Outcome.ADDED, event);
    }

    public static EventSignupResult updated(Event event) {
        return new EventSignupResult(Outcome.UPDATED, event);
    }

    public static EventSignupResult waitlisted(Event event) {
        return new EventSignupResult(Outcome.WAITLISTED, event);
    }

    public static EventSignupResult withoutEvent(Outcome outcome) {
        return new EventSignupResult(outcome, null);
    }
}
