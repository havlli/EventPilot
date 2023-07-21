package com.github.havlli.EventPilot.command.createevent;

import com.github.havlli.EventPilot.command.SlashCommand;
import com.github.havlli.EventPilot.core.SimplePermissionChecker;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.rest.util.Permission;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class CreateEventCommand implements SlashCommand {

    private final CreateEventInteraction createEventInteraction;
    private Class<? extends Event> eventType = ChatInputInteractionEvent.class;

    public CreateEventCommand(CreateEventInteraction createEventInteraction) {
        this.createEventInteraction = createEventInteraction;
    }

    @Override
    public String getName() {
        return "create-event";
    }

    @Override
    public Class<? extends Event> getEventType() {
        return eventType;
    }

    @Override
    public void setEventType(Class<? extends Event> eventType) {
        this.eventType = eventType;
    }

    @Override
    public Mono<?> handle(Event event) {
        ChatInputInteractionEvent interactionEvent = (ChatInputInteractionEvent) event;
        if (!interactionEvent.getCommandName().equals(getName())) {
            return Mono.empty();
        }

        SimplePermissionChecker permissionChecker = new SimplePermissionChecker(interactionEvent, Permission.MANAGE_CHANNELS);

        Snowflake guildId = interactionEvent.getInteraction().getGuildId().orElse(Snowflake.of(0));

        return interactionEvent.deferReply()
                .withEphemeral(true)
                .then(permissionChecker.followup(followupMessage(interactionEvent, guildId)));
    }

    private Mono<Message> followupMessage(ChatInputInteractionEvent event, Snowflake guildId) {
        String prompt = "Initiated process of creating event in your DMs, please continue there!";
        return event.createFollowup(prompt)
                .withEphemeral(true)
                .flatMap(ignored -> createEventInteraction.start(event, guildId));
    }
}
