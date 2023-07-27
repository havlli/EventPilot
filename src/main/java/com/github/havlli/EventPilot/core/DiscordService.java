package com.github.havlli.EventPilot.core;

import com.github.havlli.EventPilot.component.selectmenu.RaidSelectMenu;
import com.github.havlli.EventPilot.entity.event.Event;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.spec.MessageEditSpec;
import discord4j.rest.http.client.ClientException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class DiscordService {

    private final GatewayDiscordClient client;

    public DiscordService(GatewayDiscordClient client) {
        this.client = client;
    }

    public void deactivateEvents(List<Event> events) {

        MessageEditSpec editedMessage = MessageEditSpec.builder()
                .addComponent(new RaidSelectMenu().getDisabledRow())
                .build();

        events.forEach(event -> {
            Snowflake messageId = Snowflake.of(event.getEventId());
            Snowflake channelId = Snowflake.of(event.getDestinationChannelId());

            client.getMessageById(channelId, messageId)
                    .flatMap(message -> message.edit(editedMessage))
                    .onErrorResume(ClientException.class, e -> {
                        // TODO: implement logging for catching 404
                        System.out.println(e.getMessage());
                        return Mono.empty();
                    })
                    .subscribe();
        });
    }
}
