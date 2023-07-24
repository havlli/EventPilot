package com.github.havlli.EventPilot.command.createevent;

import com.github.havlli.EventPilot.component.ButtonRow;
import com.github.havlli.EventPilot.component.selectmenu.ChannelSelectMenu;
import com.github.havlli.EventPilot.component.selectmenu.MemberSizeSelectMenu;
import com.github.havlli.EventPilot.component.selectmenu.RaidSelectMenu;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.generator.EmbedGenerator;
import com.github.havlli.EventPilot.prompt.MessageCollector;
import com.github.havlli.EventPilot.prompt.PromptFilter;
import com.github.havlli.EventPilot.prompt.PromptFormatter;
import com.github.havlli.EventPilot.prompt.PromptService;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.InteractionReplyEditSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
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
    private final PromptService promptService;
    private final EmbedGenerator embedGenerator;
    private ChatInputInteractionEvent initialEvent;
    private Snowflake guildId;
    private User user;
    private Mono<PrivateChannel> privateChannelMono;
    private Event.Builder eventBuilder;


    public CreateEventInteraction(
            GatewayDiscordClient client,
            MessageCollector messageCollector,
            PromptFormatter promptFormatter,
            PromptFilter promptFilter,
            PromptService promptService,
            EmbedGenerator embedGenerator) {
        this.client = client;
        this.messageCollector = messageCollector;
        this.promptFormatter = promptFormatter;
        this.promptFilter = promptFilter;
        this.promptService = promptService;
        this.embedGenerator = embedGenerator;
    }

    public Mono<Message> start(ChatInputInteractionEvent event) {
        this.initialEvent = event;
        this.guildId = promptService.fetchGuildId(initialEvent);
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
        List<TextChannel> textChannels = promptService.fetchGuildTextChannels(initialEvent);
        ChannelSelectMenu channelSelectMenu = new ChannelSelectMenu(textChannels);
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
                                                .contentOrNull(result)
                                                .componentsOrNull(null)
                                                .build()))
                                        .then(promptConfirmation());
                            });
                });
    }

    private Mono<Message> promptConfirmation() {
        ButtonRow buttonRow = ButtonRow.builder()
                .addButton("confirm", "Confirm", ButtonRow.Builder.buttonType.PRIMARY)
                .addButton("cancel", "Cancel", ButtonRow.Builder.buttonType.DANGER)
                .addButton("repeat", "Start Again!", ButtonRow.Builder.buttonType.SECONDARY)
                .build();
        MessageCreateSpec prompt = MessageCreateSpec.builder()
                .addEmbed(embedGenerator.generatePreview(eventBuilder))
                .addComponent(buttonRow.getActionRow())
                .build();


        return privateChannelMono
                .flatMap(privateChannel -> privateChannel.createMessage(prompt))
                .flatMap(promptedMessage -> {
                    messageCollector.collect(promptedMessage);

                    return client.getEventDispatcher().on(ButtonInteractionEvent.class)
                            .filter(promptFilter.buttonInteractionEvent(buttonRow, user))
                            .next()
                            .flatMap(event -> {
                                if (event.getCustomId().equals("confirm")) {

                                    return event.deferReply()
                                            .then(finalizePrompt())
                                            .then(messageCollector.cleanup())
                                            .then(event.getInteractionResponse().deleteInitialResponse())
                                            .then(Mono.empty());

                                } else if (event.getCustomId().equals("cancel")) {

                                    return event.deferReply()
                                            .then(messageCollector.cleanup())
                                            .then(event.getInteractionResponse().deleteInitialResponse())
                                            .then(Mono.empty());

                                } else if (event.getCustomId().equals("repeat")) {

                                    return event.deferReply()
                                            .then(messageCollector.cleanup())
                                            .then(event.getInteractionResponse().deleteInitialResponse())
                                            .then(promptName());
                                }

                                return Mono.empty();
                            });
                });
    }

    private Mono<Message> finalizePrompt() {
        Snowflake destinationChannel = Snowflake.of(eventBuilder.getDestinationChannelId());
        return initialEvent.getInteraction()
                .getGuild()
                .flatMap(guild -> guild.getChannelById(destinationChannel)
                        .cast(MessageChannel.class)
                        .flatMap(channel -> channel.createMessage("Generating event..."))
                        .flatMap(message -> {
                            Snowflake messageId = message.getId();
                            eventBuilder.withEventId(messageId.asString());

                            Event event = eventBuilder.build();
                            System.out.println(event);

                            String messageUrl = promptFormatter.messageUrl(guildId, destinationChannel, messageId);
                            Mono<Message> finalMessage = privateChannelMono
                                    .flatMap(channel -> channel.createMessage("Event created in " + messageUrl));

                            MessageEditSpec finalEmbed = MessageEditSpec.builder()
                                    .contentOrNull(null)
                                    .addEmbed(embedGenerator.generateEmbed(event))
                                    .addAllComponents(embedGenerator.generateComponents(event.getEventId()))
                                    .build();

                            return message.edit(finalEmbed)
                                    .then(finalMessage);
                        })
                );
    }
}
