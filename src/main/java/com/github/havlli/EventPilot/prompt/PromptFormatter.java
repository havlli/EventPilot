package com.github.havlli.EventPilot.prompt;

import com.github.havlli.EventPilot.entity.event.Event;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import org.springframework.stereotype.Component;

@Component
public class PromptFormatter {

    public String formatResponse(SelectMenuInteractionEvent event) {
        return String.join(", ", event.getValues());
    }

    public String messageUrl(Event event) {
        return messageUrl(
                Snowflake.of(event.getGuild().getId()),
                Snowflake.of(event.getDestinationChannelId()),
                Snowflake.of(event.getEventId())
        );
    }

    public String messageUrl(Snowflake guildId, Snowflake channelId, Snowflake messageId) {
        return String.format(
                "https://discord.com/channels/%s/%s/%s",
                guildId.asString(),
                channelId.asString(),
                messageId.asString()
        );
    }

    public String channelUrl(Snowflake guildId, Snowflake channelId) {
        return String.format(
                "https://discord.com/channels/%s/%s",
                guildId.asString(),
                channelId.asString()
        );
    }
}
