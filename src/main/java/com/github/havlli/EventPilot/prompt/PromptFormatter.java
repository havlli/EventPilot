package com.github.havlli.EventPilot.prompt;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class PromptFormatter {

    public String formatResponse(SelectMenuInteractionEvent event) {
        Objects.requireNonNull(event);

        return String.join(", ", event.getValues());
    }

    public String messageUrl(Snowflake guildId, Snowflake channelId, Snowflake messageId) {
        Objects.requireNonNull(guildId);
        Objects.requireNonNull(channelId);
        Objects.requireNonNull(messageId);

        return String.format(
                "https://discord.com/channels/%s/%s/%s",
                guildId.asString(),
                channelId.asString(),
                messageId.asString()
        );
    }

    public String channelUrl(Snowflake guildId, Snowflake channelId) {
        Objects.requireNonNull(guildId);
        Objects.requireNonNull(channelId);

        return String.format(
                "https://discord.com/channels/%s/%s",
                guildId.asString(),
                channelId.asString()
        );
    }
}
