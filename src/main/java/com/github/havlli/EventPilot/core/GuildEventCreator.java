package com.github.havlli.EventPilot.core;

import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.prompt.PromptFormatter;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.ScheduledEvent;
import discord4j.core.spec.ScheduledEventCreateSpec;
import discord4j.core.spec.ScheduledEventEntityMetadataSpec;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class GuildEventCreator {

    private final GatewayDiscordClient client;
    private final PromptFormatter formatter;

    public GuildEventCreator(GatewayDiscordClient client, PromptFormatter formatter) {
        this.client = client;
        this.formatter = formatter;
    }

    public Mono<ScheduledEvent> createScheduledEvent(Event event) {
        Snowflake guildId = extractGuildId(event);
        return client.getGuildById(guildId)
                .flatMap(guild -> guild.createScheduledEvent(scheduledEventCreateSpec(event)));
    }

    protected ScheduledEventCreateSpec scheduledEventCreateSpec(Event event) {
        return ScheduledEventCreateSpec.builder()
                .privacyLevel(ScheduledEvent.PrivacyLevel.GUILD_ONLY)
                .entityType(ScheduledEvent.EntityType.EXTERNAL)
                .entityMetadata(scheduledEventEntityMetadataSpec(event))
                .name(event.getName())
                .description(event.getDescription())
                .scheduledStartTime(event.getDateTime())
                .scheduledEndTime(event.getDateTime().plusSeconds(14400))
                .build();
    }

    protected ScheduledEventEntityMetadataSpec scheduledEventEntityMetadataSpec(Event event) {
        return ScheduledEventEntityMetadataSpec.builder()
                .location(formatter.messageUrl(event))
                .build();
    }

    private Snowflake extractGuildId(Event event) {
        return Snowflake.of(event.getGuild().getId());
    }
}
