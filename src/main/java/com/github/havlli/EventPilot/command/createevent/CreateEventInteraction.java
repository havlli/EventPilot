package com.github.havlli.EventPilot.command.createevent;

import com.github.havlli.EventPilot.prompt.MessageCollector;
import com.github.havlli.EventPilot.prompt.PromptFormatter;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.PrivateChannel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Component
public class CreateEventInteraction {
    private final GatewayDiscordClient client;
    private final MessageCollector messageCollector;
    private final PromptFormatter promptFormatter;
    private ChatInputInteractionEvent initialEvent;
    private Snowflake guildId;
    private User user;
    private Mono<PrivateChannel> privateChannelMono;


    public CreateEventInteraction(GatewayDiscordClient client, MessageCollector messageCollector, PromptFormatter promptFormatter) {
        this.client = client;
        this.messageCollector = messageCollector;
        this.promptFormatter = promptFormatter;
    }

    public Mono<Message> start(
            ChatInputInteractionEvent event,
            Snowflake guildId
    ) {
        this.initialEvent = event;
        this.guildId = guildId;
        this.user = event.getInteraction().getUser();
        this.privateChannelMono = user.getPrivateChannel();

        return chainedPrompts();
    }

    private Mono<Message> chainedPrompts() {
        return promptName()
                .flatMap(ignore -> promptDescription())
                .flatMap(ignore -> promptDateTime());
    }

    private Mono<Message> promptName() {
        String prompt = "**Step 1**\nEnter name for your event!";
        return privateChannelMono
                .flatMap(privateChannel -> privateChannel.createMessage(prompt))
                .flatMap(promptedMessage -> {
                    messageCollector.collect(promptedMessage);
                    return client.getEventDispatcher().on(MessageCreateEvent.class)
                            .filter(event -> event.getMessage().getAuthor().equals(Optional.of(user)))
                            .next()
                            .flatMap(event -> {
                                System.out.println(event.getMessage().getContent());
                                return Mono.just(event.getMessage());
                            });
                })
                .flatMap(ignore -> promptDescription());
    }

    private Mono<Message> promptDescription() {
        String prompt = "**Step 2**\nEnter description";
        return privateChannelMono
                .flatMap(privateChannel -> privateChannel.createMessage(prompt))
                .flatMap(promptedMessage -> {
                    messageCollector.collect(promptedMessage);
                    return client.getEventDispatcher().on(MessageCreateEvent.class)
                            .filter(event -> event.getMessage().getAuthor().equals(Optional.of(user)))
                            .next()
                            .flatMap(event -> {
                                System.out.println(event.getMessage().getContent());
                                return Mono.just(event.getMessage());
                            });
                })
                .flatMap(ignore -> promptDateTime());
    }

    private Mono<Message> promptDateTime() {
        String prompt = "**Step 3**\nEnter the date and time in UTC timezone (format: dd.MM.yyyy HH:mm)";
        return privateChannelMono
                .flatMap(privateChannel -> privateChannel.createMessage(prompt))
                .flatMap(promptedMessage -> {
                    messageCollector.collect(promptedMessage);
                    return client.getEventDispatcher().on(MessageCreateEvent.class)
                            .filter(event -> event.getMessage().getAuthor().equals(Optional.of(user)))
                            .next()
                            .flatMap(event -> {
                                String messageContent = event.getMessage().getContent();
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                                LocalDateTime localDateTime = LocalDateTime.parse(messageContent, formatter);
                                System.out.println(event.getMessage().getContent());
                                System.out.println(localDateTime);
                                return Mono.just(event.getMessage());
                            })
                            .onErrorResume(DateTimeParseException.class, error -> {
                                String errorMessage = "**Error** Invalid format: %s".formatted(error.getParsedString());
                                return privateChannelMono.flatMap(channel -> channel.createMessage(errorMessage)
                                        .flatMap(message -> {
                                            messageCollector.collect(message);
                                            return Mono.just(message);
                                        })
                                ).then(promptDateTime());
                            });
                })
                .flatMap(ignore -> promptRaidSelect());
    }

    private Mono<Message> promptRaidSelect() {
        String prompt = "**Step 5**\nChoose which raids is this signup for:\nRequired 1 selection, maximum 3";
        return privateChannelMono
                .flatMap(privateChannel -> privateChannel.createMessage(prompt))
                .flatMap(promptedMessage -> {
                    messageCollector.collect(promptedMessage);
                    return client.getEventDispatcher().on(MessageCreateEvent.class)
                            .filter(event -> event.getMessage().getAuthor().equals(Optional.of(user)))
                            .next()
                            .flatMap(event -> {
                                System.out.println(event.getMessage().getContent());
                                return Mono.just(event.getMessage());
                            });
                });
    }
}
