package com.github.havlli.EventPilot.entity.event;

import com.github.havlli.EventPilot.entity.guild.Guild;
import com.github.havlli.EventPilot.entity.participant.Participant;
import com.github.havlli.EventPilot.generator.EmbedPreviewable;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Entity
@Table(name = "event")
public class Event {

    @Id
    @Column(name = "id")
    private String eventId;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "author", nullable = false)
    private String author;
    @Column(name = "date_time", nullable = false)
    private LocalDateTime dateTime;
    @Column(name = "dest_channel", nullable = false)
    private String destinationChannelId;
    @Transient
    private String instances;
    @Column(name = "member_size", nullable = false)
    private String memberSize;
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Participant> participants;
    @ManyToOne
    @JoinColumn(name = "guild_id")
    private Guild guild;
    public Event() { }

    public Event(
            String eventId,
            String name,
            String description,
            String author,
            LocalDateTime dateTime,
            String destinationChannelId,
            String instances,
            String memberSize,
            List<Participant> participants
    ) {
        this.eventId = eventId;
        this.name = name;
        this.description = description;
        this.author = author;
        this.dateTime = dateTime;
        this.destinationChannelId = destinationChannelId;
        this.instances = instances;
        this.memberSize = memberSize;
        this.participants = participants;
    }

    public Event(
            String eventId,
            String name,
            String description,
            String author,
            LocalDateTime dateTime,
            String destinationChannelId,
            String instances,
            String memberSize,
            List<Participant> participants,
            Guild guild
    ) {
        this(eventId, name, description, author, dateTime, destinationChannelId, instances, memberSize, participants);
        this.guild = guild;
    }

    public Event(Event.Builder builder) {
        this.eventId = builder.eventId;
        this.name = builder.name;
        this.description = builder.description;
        this.author = builder.author;
        this.dateTime = builder.dateTime;
        this.destinationChannelId = builder.destinationChannelId;
        this.instances = builder.instances;
        this.memberSize = builder.memberSize;
        this.guild = builder.guild;
        this.participants = new ArrayList<>();
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

    public List<Participant> getParticipants() {
        return participants;
    }

    public void setParticipants(List<Participant> participants) {
        this.participants = participants;
    }

    public Guild getGuild() {
        return guild;
    }

    public void setGuild(Guild guild) {
        this.guild = guild;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Objects.equals(eventId, event.eventId) && Objects.equals(name, event.name) && Objects.equals(description, event.description) && Objects.equals(author, event.author) && Objects.equals(dateTime, event.dateTime) && Objects.equals(destinationChannelId, event.destinationChannelId) && Objects.equals(instances, event.instances) && Objects.equals(memberSize, event.memberSize) && Objects.equals(participants, event.participants) && Objects.equals(guild, event.guild);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, name, description, author, dateTime, destinationChannelId, instances, memberSize, participants, guild);
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
                ", participants=" + participants +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder implements EmbedPreviewable {
        private Event event;
        private String eventId;
        private String name;
        private String description;
        private String author;
        private LocalDateTime dateTime;
        private String destinationChannelId;
        private String instances;
        private String memberSize;
        private Guild guild;

        private Builder() { }

        public Builder withEventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withAuthor(String authorName) {
            this.author = authorName;
            return this;
        }

        public Builder withDateTime(String date, String time) {
            LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalTime localTime = LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
            this.dateTime = LocalDateTime.of(localDate, localTime);
            return this;
        }

        public Builder withDateTime(LocalDateTime localDateTime) {
            this.dateTime = localDateTime;
            return this;
        }

        public Builder withDestinationChannel(String channelId) {
            this.destinationChannelId = channelId;
            return this;
        }

        public Builder withInstances(List<String> instances) {
            this.instances = String.join(", ", instances);
            return this;
        }

        public Builder withMemberSize(String memberSize) {
            this.memberSize = memberSize;
            return this;
        }

        public Builder withGuild(Guild guild) {
            this.guild = guild;
            return this;
        }

        public String getDestinationChannelId() {
            return destinationChannelId;
        }

        public Event build() {
            this.event = new Event(this);
            return this.event;
        }

        public Event getEvent() {
            return this.event;
        }

        @Override
        public HashMap<String, String> previewFields() {
            return new HashMap<>(Map.of(
                    "name", name,
                    "description", description,
                    "date and time", dateTime.toString(),
                    "instances", instances,
                    "member size", memberSize,
                    "destination channel", destinationChannelId
            ));
        }
    }
}
