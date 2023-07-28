package com.github.havlli.EventPilot.core;

import com.github.havlli.EventPilot.component.selectmenu.ExpiredSelectMenu;
import com.github.havlli.EventPilot.entity.event.Event;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageEditSpec;
import discord4j.rest.http.client.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Service
public class DiscordService {

    private static final Logger logger = LoggerFactory.getLogger(DiscordService.class);
    private final GatewayDiscordClient client;

    public DiscordService(GatewayDiscordClient client) {
        this.client = client;
    }

    public void deactivateEvents(List<Event> events) {

        MessageEditSpec editedMessage = MessageEditSpec.builder()
                .addComponent(new ExpiredSelectMenu().getActionRow())
                .build();

        events.forEach(event -> {
            Snowflake messageId = Snowflake.of(event.getEventId());
            Snowflake channelId = Snowflake.of(event.getDestinationChannelId());

            client.getMessageById(channelId, messageId)
                    .flatMap(message -> {

                        if (!isAlreadyDeactivated(message)) return message.edit(editedMessage);

                        return Mono.empty();
                    })
                    .onErrorResume(ClientException.class, e -> {

                        String errorMessage = "Message {%s} was not found in channel {%s}"
                                .formatted(messageId.asString(), channelId.asString());
                        logger.error(errorMessage, e);

                        return Mono.empty();
                    })
                    .subscribe();
        });
    }

    private boolean isAlreadyDeactivated(Message message) {
        Optional<String> customId = message.getComponents().stream()
                .findFirst()
                .flatMap(layoutComponent -> layoutComponent.getChildren().stream()
                        .findFirst())
                .flatMap(messageComponent -> messageComponent
                        .getData()
                        .customId()
                        .toOptional());

        if (customId.isEmpty()) return false;

        return customId.get().equals("expired");
    }
}
