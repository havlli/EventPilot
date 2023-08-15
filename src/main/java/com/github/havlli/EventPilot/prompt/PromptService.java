package com.github.havlli.EventPilot.prompt;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.TextChannel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class PromptService {

    private final GatewayDiscordClient client;

    public PromptService(GatewayDiscordClient client) {
        this.client = client;
    }

    public Flux<TextChannel> fetchGuildTextChannels(InteractionCreateEvent event) {
        return event.getInteraction()
                .getGuild()
                .map(Guild::getId)
                .flatMapMany(guildId -> client.getGuildChannels(guildId).ofType(TextChannel.class));
    }

    public Snowflake fetchGuildId(InteractionCreateEvent event) {
        return event.getInteraction().getGuildId()
                .orElse(Snowflake.of(0));
    }
}
