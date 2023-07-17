package com.github.havlli.EventPilot.entity.event;

import java.time.LocalDateTime;
import java.util.Objects;

public class Event {

    private String eventId;
    private String name;
    private String description;
    private String author;
    private LocalDateTime dateTime;
    private String destinationChannelId;
    private String instances;
    private String memberSize;

    public Event() { }

    public Event(
            String eventId,
            String name,
            String description,
            String author,
            LocalDateTime dateTime,
            String destinationChannelId,
            String instances,
            String memberSize
    ) {
        this.eventId = eventId;
        this.name = name;
        this.description = description;
        this.author = author;
        this.dateTime = dateTime;
        this.destinationChannelId = destinationChannelId;
        this.instances = instances;
        this.memberSize = memberSize;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getDestinationChannelId() {
        return destinationChannelId;
    }

    public void setDestinationChannelId(String destinationChannelId) {
        this.destinationChannelId = destinationChannelId;
    }

    public String getInstances() {
        return instances;
    }

    public void setInstances(String instances) {
        this.instances = instances;
    }

    public String getMemberSize() {
        return memberSize;
    }

    public void setMemberSize(String memberSize) {
        this.memberSize = memberSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Objects.equals(eventId, event.eventId) && Objects.equals(name, event.name) && Objects.equals(description, event.description) && Objects.equals(author, event.author) && Objects.equals(dateTime, event.dateTime) && Objects.equals(destinationChannelId, event.destinationChannelId) && Objects.equals(instances, event.instances) && Objects.equals(memberSize, event.memberSize);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, name, description, author, dateTime, destinationChannelId, instances, memberSize);
    }

    @Override
    public String toString() {
        return "Event{" +
                "eventId='" + eventId + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", author='" + author + '\'' +
                ", dateTime=" + dateTime +
                ", destinationChannelId='" + destinationChannelId + '\'' +
                ", instances='" + instances + '\'' +
                ", memberSize='" + memberSize + '\'' +
                '}';
    }
}
