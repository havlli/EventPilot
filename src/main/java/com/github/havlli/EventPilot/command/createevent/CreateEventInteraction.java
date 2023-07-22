package com.github.havlli.EventPilot.command.createevent;

import com.github.havlli.EventPilot.component.selectmenu.ChannelSelectMenu;
import com.github.havlli.EventPilot.component.selectmenu.MemberSizeSelectMenu;
import com.github.havlli.EventPilot.component.selectmenu.RaidSelectMenu;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.prompt.MessageCollector;
import com.github.havlli.EventPilot.prompt.PromptFilter;
import com.github.havlli.EventPilot.prompt.PromptFormatter;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.InteractionReplyEditSpec;
import discord4j.core.spec.MessageCreateSpec;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Component
public class CreateEventInteraction {
    private final GatewayDiscordClient client;
    private final MessageCollector messageCollector;
    private final PromptFormatter promptFormatter;
    private final PromptFilter promptFilter;
    private ChatInputInteractionEvent initialEvent;
    private Snowflake guildId;
    private User user;
    private Mono<PrivateChannel> privateChannelMono;
    private Event.Builder eventBuilder;


    public CreateEventInteraction(
            GatewayDiscordClient client,
            MessageCollector messageCollector,
            PromptFormatter promptFormatter,
            PromptFilter promptFilter
    ) {
        this.client = client;
        this.messageCollector = messageCollector;
        this.promptFormatter = promptFormatter;
        this.promptFilter = promptFilter;
    }

    public Mono<Message> start(ChatInputInteractionEvent event, Snowflake guildId) {
        this.initialEvent = event;
        this.guildId = guildId;
        this.user = initialEvent.getInteraction().getUser();
        this.privateChannelMono = user.getPrivateChannel();
        this.eventBuilder = Event.builder();

        eventBuilder.withAuthor(user.getUsername());

        return promptName();
    }

    private Mono<Message> promptName() {
        String prompt = "**Step 1**\nEnter name for your event!";
        return privateChannelMono
                .flatMap(privateChannel -> privateChannel.createMessage(prompt))
                .flatMap(promptedMessage -> {
                    messageCollector.collect(promptedMessage);

                    return client.getEventDispatcher().on(MessageCreateEvent.class)
                            .filter(promptFilter.isMessageAuthor(user))
                            .next()
                            .flatMap(event -> {
                                eventBuilder.withName(event.getMessage().getContent());
                                System.out.println(event.getMessage().getContent());

                                return promptDescription();
                            });
                });
    }

    private Mono<Message> promptDescription() {
        String prompt = "**Step 2**\nEnter description";
        return privateChannelMono
                .flatMap(privateChannel -> privateChannel.createMessage(prompt))
                .flatMap(promptedMessage -> {
                    messageCollector.collect(promptedMessage);

                    return client.getEventDispatcher().on(MessageCreateEvent.class)
                            .filter(promptFilter.isMessageAuthor(user))
                            .next()
                            .flatMap(event -> {
                                eventBuilder.withDescription(event.getMessage().getContent());
                                System.out.println(event.getMessage().getContent());
                                return promptDateTime();
                            });
                });
    }

    private Mono<Message> promptDateTime() {
        String prompt = "**Step 3**\nEnter the date and time in UTC timezone (format: dd.MM.yyyy HH:mm)";
        return privateChannelMono
                .flatMap(privateChannel -> privateChannel.createMessage(prompt))
                .flatMap(promptedMessage -> {
                    messageCollector.collect(promptedMessage);

                    return client.getEventDispatcher().on(MessageCreateEvent.class)
                            .filter(promptFilter.isMessageAuthor(user))
                            .next()
                            .flatMap(event -> {
                                String messageContent = event.getMessage().getContent();
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                                LocalDateTime localDateTime = LocalDateTime.parse(messageContent, formatter);
                                eventBuilder.withDateTime(localDateTime);
                                System.out.println(event.getMessage().getContent());
                                System.out.println(localDateTime);

                                return promptRaidSelect();
                            })
                            .onErrorResume(DateTimeParseException.class, error -> {
                                String errorMessage = "**Error** Invalid format: %s".formatted(error.getParsedString());

                                return privateChannelMono.flatMap(channel -> channel.createMessage(errorMessage)
                                        .flatMap(message -> {
                                            messageCollector.collect(message);
                                            return Mono.empty();
                                        })
                                ).then(promptDateTime());
                            });
                });
    }

    private Mono<Message> promptRaidSelect() {

        RaidSelectMenu raidSelectMenu = new RaidSelectMenu();
        MessageCreateSpec prompt = MessageCreateSpec.builder()
                .content("**Step 4**\nChoose which raids is this signup for:\nRequired 1 selection, maximum 3")
                .addComponent(raidSelectMenu.getActionRow())
                .build();

        return privateChannelMono
                .flatMap(privateChannel -> privateChannel.createMessage(prompt))
                .flatMap(promptedMessage -> {
                    messageCollector.collect(promptedMessage);

                    return client.getEventDispatcher().on(SelectMenuInteractionEvent.class)
                            .filter(promptFilter.selectInteractionEvent(raidSelectMenu, user))
                            .next()
                            .flatMap(event -> {
                                eventBuilder.withInstances(event.getValues());
                                List<String> result = event.getValues();
                                System.out.println(result);

                                return event.deferEdit()
                                        .then(event.editReply(InteractionReplyEditSpec.builder()
                                                .components(List.of(raidSelectMenu.getDisabledRow()))
                                                .build()))
                                        .then(promptMemberSize());
                            });
                });
    }

    private Mono<Message> promptMemberSize() {
        MemberSizeSelectMenu memberSizeSelectMenu = new MemberSizeSelectMenu();
        String defaultSize = "25";
        MessageCreateSpec prompt = MessageCreateSpec.builder()
                .content("**Step 5**\nChoose maximum attendants count for this event")
                .addComponent(memberSizeSelectMenu.getActionRow())
                .build();

        return privateChannelMono
                .flatMap(privateChannel -> privateChannel.createMessage(prompt))
                .flatMap(promptedMessage -> {
                    messageCollector.collect(promptedMessage);

                    return client.getEventDispatcher().on(SelectMenuInteractionEvent.class)
                            .filter(promptFilter.selectInteractionEvent(memberSizeSelectMenu, user))
                            .next()
                            .flatMap(event -> {
                                String result = event.getValues().stream().findFirst().orElse(defaultSize);
                                eventBuilder.withMemberSize(result);
                                System.out.println(result);

                                return event.deferEdit()
                                        .then(event.editReply(InteractionReplyEditSpec.builder()
                                                .contentOrNull(result)
                                                .componentsOrNull(null)
                                                .build()))
                                        .then(promptDestinationChannel());
                            });
                });
    }

    private Mono<Message> promptDestinationChannel() {
        ChannelSelectMenu channelSelectMenu = new ChannelSelectMenu(fetchGuildTextChannels());
        MessageCreateSpec prompt = MessageCreateSpec.builder()
                .content("**Step 6**\nChoose in which channel post this raid signup")
                .addComponent(channelSelectMenu.getActionRow())
                .build();
        Snowflake originChannelId = initialEvent.getInteraction().getChannelId();

        return privateChannelMono
                .flatMap(privateChannel -> privateChannel.createMessage(prompt))
                .flatMap(promptedMessage -> {
                    messageCollector.collect(promptedMessage);

                    return client.getEventDispatcher().on(SelectMenuInteractionEvent.class)
                            .filter(promptFilter.selectInteractionEvent(channelSelectMenu, user))
                            .next()
                            .flatMap(event -> {
                                String result = event.getValues().stream()
                                        .findFirst()
                                        .orElse(originChannelId.asString());

                                eventBuilder.withDestinationChannel(result);
                                System.out.println(result);
                                System.out.println(originChannelId.asString());

                                return event.deferEdit()
                                        .then(event.editReply(InteractionReplyEditSpec.builder()
                                                .contentOrNull(result.toString())
                                                .componentsOrNull(null)
                                                .build()));
                            });
                });
    }

    private List<TextChannel> fetchGuildTextChannels() {
        return initialEvent.getInteraction()
                .getGuild()
                .map(Guild::getId)
                .flatMapMany(guildId -> client.getGuildChannels(guildId).ofType(TextChannel.class))
                .collectList()
                .block();
    }
}
