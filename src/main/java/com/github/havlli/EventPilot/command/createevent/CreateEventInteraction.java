package com.github.havlli.EventPilot.command.createevent;

import com.github.havlli.EventPilot.component.ButtonRow;
import com.github.havlli.EventPilot.component.selectmenu.ChannelSelectMenu;
import com.github.havlli.EventPilot.component.selectmenu.MemberSizeSelectMenu;
import com.github.havlli.EventPilot.component.selectmenu.RaidSelectMenu;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventService;
import com.github.havlli.EventPilot.entity.guild.Guild;
import com.github.havlli.EventPilot.entity.guild.GuildService;
import com.github.havlli.EventPilot.generator.EmbedGenerator;
import com.github.havlli.EventPilot.prompt.*;
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
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    private final EventService eventService;
    private final GuildService guildService;
    private ChatInputInteractionEvent initialEvent;
    private User user;
    private Mono<PrivateChannel> privateChannelMono;
    private Event.Builder eventBuilder;


    public CreateEventInteraction(
            GatewayDiscordClient client,
            MessageCollector messageCollector,
            PromptFormatter promptFormatter,
            PromptFilter promptFilter,
            PromptService promptService,
            EmbedGenerator embedGenerator,
            EventService eventService,
            GuildService guildService) {
        this.client = client;
        this.messageCollector = messageCollector;
        this.promptFormatter = promptFormatter;
        this.promptFilter = promptFilter;
        this.promptService = promptService;
        this.embedGenerator = embedGenerator;
        this.eventService = eventService;
        this.guildService = guildService;
    }

    public Mono<Message> start(ChatInputInteractionEvent event) {
        this.initialEvent = event;
        this.user = initialEvent.getInteraction().getUser();
        this.privateChannelMono = user.getPrivateChannel();

        Snowflake guildId = promptService.fetchGuildId(initialEvent);
        Guild guild = guildService.getGuildById(guildId.asString());

        this.eventBuilder = Event.builder();
        eventBuilder.withAuthor(user.getUsername());
        eventBuilder.withGuild(guild);

        return promptName()
                .flatMap(ignored -> promptDescription())
                .flatMap(ignored -> promptDateTime())
                .flatMap(ignored -> promptRaidSelect())
                .flatMap(ignored -> promptMemberSize())
                .flatMap(ignored -> promptDestinationChannel())
                .flatMap(ignored -> promptConfirmation())
                .flatMap(confirmation -> {
                    System.out.println("Last flatmap " + confirmation.getCustomId());
                    return Mono.empty();
                })
                .then(Mono.empty());
    }

    private Mono<MessageCreateEvent> promptName() {
        String prompt = "**Step 1**\nEnter name for your event!";

        return new TextPromptMono.Builder<>(client, MessageCreateEvent.class)
                .messageChannel(privateChannelMono)
                .messageCreateSpec(MessageCreateSpec.builder().content(prompt).build())
                .withMessageCollector(messageCollector)
                .eventPredicate(promptFilter.isMessageAuthor(user))
                .eventProcessor(event -> {
                    eventBuilder.withName(event.getMessage().getContent());
                    System.out.println(event.getMessage().getContent());
                })
                .build()
                .mono();
    }

    private Mono<MessageCreateEvent> promptDescription() {
        String prompt = "**Step 2**\nEnter description";

        return new TextPromptMono.Builder<>(client, MessageCreateEvent.class)
                .messageChannel(privateChannelMono)
                .messageCreateSpec(MessageCreateSpec.builder().content(prompt).build())
                .withMessageCollector(messageCollector)
                .eventPredicate(promptFilter.isMessageAuthor(user))
                .eventProcessor(event -> {
                    eventBuilder.withDescription(event.getMessage().getContent());
                    System.out.println(event.getMessage().getContent());
                })
                .build()
                .mono();
    }

    private Mono<MessageCreateEvent> promptDateTime() {
        String prompt = "**Step 3**\nEnter the date and time in UTC timezone (format: dd.MM.yyyy HH:mm)";

        return new TextPromptMono.Builder<>(client, MessageCreateEvent.class)
                .messageChannel(privateChannelMono)
                .messageCreateSpec(MessageCreateSpec.builder().content(prompt).build())
                .withMessageCollector(messageCollector)
                .eventPredicate(promptFilter.isMessageAuthor(user))
                .eventProcessor(event -> {
                    String messageContent = event.getMessage().getContent();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                    LocalDateTime localDateTime = LocalDateTime.parse(messageContent, formatter);
                    Instant instant = localDateTime.atZone(ZoneOffset.UTC).toInstant();
                    eventBuilder.withDateTime(instant);
                    System.out.println(event.getMessage().getContent());
                    System.out.println(localDateTime);
                })
                .onErrorRepeat(DateTimeParseException.class, "Invalid Format")
                .build()
                .mono();
    }

    private Mono<SelectMenuInteractionEvent> promptRaidSelect() {

        RaidSelectMenu raidSelectMenu = new RaidSelectMenu();
        MessageCreateSpec prompt = MessageCreateSpec.builder()
                .content("**Step 4**\nChoose which raids is this signup for:\nRequired 1 selection, maximum 3")
                .addComponent(raidSelectMenu.getActionRow())
                .build();

        return new TextPromptMono.Builder<>(client, SelectMenuInteractionEvent.class)
                .messageCreateSpec(prompt)
                .messageChannel(privateChannelMono)
                .withMessageCollector(messageCollector)
                .eventPredicate(promptFilter.selectInteractionEvent(raidSelectMenu, user))
                .eventProcessor(event -> {
                    eventBuilder.withInstances(event.getValues());
                    List<String> result = event.getValues();
                    System.out.println(result);
                })
                .build()
                .mono();
    }

    private Mono<SelectMenuInteractionEvent> promptMemberSize() {
        MemberSizeSelectMenu memberSizeSelectMenu = new MemberSizeSelectMenu();
        String defaultSize = "25";
        MessageCreateSpec prompt = MessageCreateSpec.builder()
                .content("**Step 5**\nChoose maximum attendants count for this event")
                .addComponent(memberSizeSelectMenu.getActionRow())
                .build();

        return new TextPromptMono.Builder<>(client, SelectMenuInteractionEvent.class)
                .messageChannel(privateChannelMono)
                .messageCreateSpec(prompt)
                .withMessageCollector(messageCollector)
                .eventPredicate(promptFilter.selectInteractionEvent(memberSizeSelectMenu, user))
                .eventProcessor(event -> {
                    String result = event.getValues().stream().findFirst().orElse(defaultSize);
                    eventBuilder.withMemberSize(result);
                })
                .build()
                .mono();
    }

    private Mono<SelectMenuInteractionEvent> promptDestinationChannel() {
        List<TextChannel> textChannels = promptService.fetchGuildTextChannels(initialEvent);
        ChannelSelectMenu channelSelectMenu = new ChannelSelectMenu(textChannels);
        MessageCreateSpec prompt = MessageCreateSpec.builder()
                .content("**Step 6**\nChoose in which channel post this raid signup")
                .addComponent(channelSelectMenu.getActionRow())
                .build();
        Snowflake originChannelId = initialEvent.getInteraction().getChannelId();

        return new TextPromptMono.Builder<>(client, SelectMenuInteractionEvent.class)
                .messageChannel(privateChannelMono)
                .messageCreateSpec(prompt)
                .withMessageCollector(messageCollector)
                .eventPredicate(promptFilter.selectInteractionEvent(channelSelectMenu, user))
                .eventProcessor(event -> {
                    String result = event.getValues().stream()
                            .findFirst()
                            .orElse(originChannelId.asString());
                    eventBuilder.withDestinationChannel(result);
                })
                .build()
                .mono();
    }

    private Mono<ButtonInteractionEvent> promptConfirmation() {
        ButtonRow buttonRow = ButtonRow.builder()
                .addButton("confirm", "Confirm", ButtonRow.Builder.buttonType.PRIMARY)
                .addButton("cancel", "Cancel", ButtonRow.Builder.buttonType.DANGER)
                .addButton("repeat", "Start Again!", ButtonRow.Builder.buttonType.SECONDARY)
                .build();
        MessageCreateSpec prompt = MessageCreateSpec.builder()
                .addEmbed(embedGenerator.generatePreview(eventBuilder))
                .addComponent(buttonRow.getActionRow())
                .build();

        return new TextPromptMono.Builder<>(client, ButtonInteractionEvent.class)
                .messageChannel(privateChannelMono)
                .messageCreateSpec(prompt)
                .withMessageCollector(messageCollector)
                .eventPredicate(promptFilter.buttonInteractionEvent(buttonRow,user))
                .eventProcessor(event -> {})
                .build()
                .mono();
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

                            String messageUrl = promptFormatter.messageUrl(guild.getId(), destinationChannel, messageId);
                            Mono<Message> finalMessage = privateChannelMono
                                    .flatMap(channel -> channel.createMessage("Event created in " + messageUrl));

                            MessageEditSpec finalEmbed = MessageEditSpec.builder()
                                    .contentOrNull(null)
                                    .addEmbed(embedGenerator.generateEmbed(event))
                                    .addAllComponents(embedGenerator.generateComponents(event.getEventId()))
                                    .build();

                            embedGenerator.subscribeInteractions(event);
                            eventService.saveEvent(event);

                            return message.edit(finalEmbed)
                                    .then(finalMessage);
                        })
                );
    }
}
