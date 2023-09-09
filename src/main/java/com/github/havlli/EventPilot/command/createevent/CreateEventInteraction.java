package com.github.havlli.EventPilot.command.createevent;

import com.github.havlli.EventPilot.component.ActionRowComponent;
import com.github.havlli.EventPilot.component.ButtonRow;
import com.github.havlli.EventPilot.component.selectmenu.ChannelSelectMenu;
import com.github.havlli.EventPilot.component.selectmenu.CustomSelectMenu;
import com.github.havlli.EventPilot.component.selectmenu.MemberSizeSelectMenu;
import com.github.havlli.EventPilot.component.selectmenu.RaidSelectMenu;
import com.github.havlli.EventPilot.entity.embedtype.EmbedType;
import com.github.havlli.EventPilot.entity.embedtype.EmbedTypeService;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventService;
import com.github.havlli.EventPilot.entity.guild.Guild;
import com.github.havlli.EventPilot.entity.guild.GuildService;
import com.github.havlli.EventPilot.exception.InvalidDateTimeException;
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
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CreateEventInteraction {

    private final static Logger LOG = LoggerFactory.getLogger(CreateEventInteraction.class);
    private final GatewayDiscordClient client;
    private final MessageCollector messageCollector;
    private final PromptFormatter promptFormatter;
    private final PromptFilter promptFilter;
    private final PromptService promptService;
    private final EmbedGenerator embedGenerator;
    private final EventService eventService;
    private final GuildService guildService;
    private final EmbedTypeService embedTypeService;
    private final TimeService timeService;
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
            GuildService guildService,
            EmbedTypeService embedTypeService,
            TimeService timeService
    ) {
        this.client = client;
        this.messageCollector = messageCollector;
        this.promptFormatter = promptFormatter;
        this.promptFilter = promptFilter;
        this.promptService = promptService;
        this.embedGenerator = embedGenerator;
        this.eventService = eventService;
        this.guildService = guildService;
        this.embedTypeService = embedTypeService;
        this.timeService = timeService;
    }

    private void initializeEventBuilder() {
        Snowflake guildId = promptService.fetchGuildId(initialEvent);
        Guild guild = guildService.getGuildById(guildId.asString());
        this.eventBuilder = Event.builder();
        eventBuilder.withAuthor(user.getUsername());
        eventBuilder.withGuild(guild);
    }

    public Mono<Message> start(ChatInputInteractionEvent event) {
        this.initialEvent = event;
        this.user = initialEvent.getInteraction().getUser();
        this.privateChannelMono = user.getPrivateChannel();
        initializeEventBuilder();

        return promptName()
                .flatMap(ignored -> promptDescription())
                .flatMap(ignored -> promptDateTime())
                .flatMap(ignored -> promptEmbedType())
                .flatMap(ignored -> promptRaidSelect())
                .flatMap(ignored -> promptMemberSize())
                .flatMap(ignored -> promptDestinationChannel())
                .flatMap(ignored -> promptConfirmationAndDeferReply())
                .flatMap(this::handleConfirmationResponse)
                .doFinally(sequenceChecker())
                .then(Mono.empty());
    }

    private Consumer<SignalType> sequenceChecker() {
        return signalType -> {
            switch (signalType) {
                case ON_COMPLETE -> LOG.info("Sequence completed successfully");
                case ON_ERROR -> LOG.info("Sequence completed with an error");
            }
        };
    }

    private Mono<?> handleConfirmationResponse(ButtonInteractionEvent event) {
        String customId = event.getCustomId();
        return switch (customId) {
            case "confirm" -> finalizeProcess()
                    .flatMapMany(ignored -> deleteAllSentMessages())
                    .then(sendCompleteDeferredInteractionSignal(event));
            case "repeat" -> deleteAllSentMessages()
                    .then(sendCompleteDeferredInteractionSignal(event))
                    .then(startOver());
            default -> deleteAllSentMessages()
                    .then(sendCompleteDeferredInteractionSignal(event));
        };
    }

    private Mono<Void> sendCompleteDeferredInteractionSignal(ButtonInteractionEvent event) {
        return event.getInteractionResponse().deleteInitialResponse();
    }

    private Flux<Void> deleteAllSentMessages() {
        return messageCollector.cleanup();
    }

    private Mono<Message> startOver() {
        return start(initialEvent);
    }

    public Mono<MessageCreateEvent> promptName() {
        String promptMessage = "**Step 1**\nEnter name for your event!";
        MessageCreateSpec messageCreateSpec = getMessageCreateSpec(promptMessage);

        return new TextPromptMono.Builder<>(client, MessageCreateEvent.class)
                .withPromptType(TextPromptMono.PromptType.DEFAULT)
                .messageChannel(privateChannelMono)
                .messageCreateSpec(messageCreateSpec)
                .withMessageCollector(messageCollector)
                .eventPredicate(promptFilter.isMessageAuthor(user))
                .eventProcessor(processNameInput())
                .build()
                .mono();
    }

    private Consumer<MessageCreateEvent> processNameInput() {
        return event -> {
            eventBuilder.withName(event.getMessage().getContent());
            System.out.println(event.getMessage().getContent());
        };
    }

    public Mono<MessageCreateEvent> promptDescription() {
        String promptMessage = "**Step 2**\nEnter description";
        MessageCreateSpec messageCreateSpec = getMessageCreateSpec(promptMessage);

        return new TextPromptMono.Builder<>(client, MessageCreateEvent.class)
                .withPromptType(TextPromptMono.PromptType.DEFAULT)
                .messageChannel(privateChannelMono)
                .messageCreateSpec(messageCreateSpec)
                .withMessageCollector(messageCollector)
                .eventPredicate(promptFilter.isMessageAuthor(user))
                .eventProcessor(processDescriptionInput())
                .build()
                .mono();
    }

    private Consumer<MessageCreateEvent> processDescriptionInput() {
        return event -> {
            eventBuilder.withDescription(event.getMessage().getContent());
            System.out.println(event.getMessage().getContent());
        };
    }

    public Mono<MessageCreateEvent> promptDateTime() {
        String promptMessage = "**Step 3**\nEnter the date and time in UTC timezone (format: dd.MM.yyyy HH:mm)";
        MessageCreateSpec messageCreateSpec = getMessageCreateSpec(promptMessage);

        return new TextPromptMono.Builder<>(client, MessageCreateEvent.class)
                .withPromptType(TextPromptMono.PromptType.DEFAULT)
                .messageChannel(privateChannelMono)
                .messageCreateSpec(messageCreateSpec)
                .withMessageCollector(messageCollector)
                .eventPredicate(promptFilter.isMessageAuthor(user))
                .eventProcessor(processDateTimeInput())
                .onErrorRepeat(DateTimeParseException.class, "Invalid Format")
                .build()
                .mono()
                .onErrorResume(InvalidDateTimeException.class, sendMessageToUserAndRepeatOnException());
    }

    private Consumer<MessageCreateEvent> processDateTimeInput() {
        return event -> {
            String messageContent = event.getMessage().getContent();
            Instant instant = timeService.parseUtcInstant(messageContent, "dd.MM.yyyy HH:mm");
            timeService.isValidFutureTime(instant);
            eventBuilder.withDateTime(instant);
        };
    }

    private Function<InvalidDateTimeException, Mono<MessageCreateEvent>> sendMessageToUserAndRepeatOnException() {
        return e -> privateChannelMono
                .flatMap(channel -> channel.createMessage("Date and time of the event has to be in future!"))
                .flatMap(message -> {
                    System.out.println(InvalidDateTimeException.class.getCanonicalName() + " triggered");
                    messageCollector.collect(message);
                    return promptDateTime();
                });
    }

    public Mono<SelectMenuInteractionEvent> promptEmbedType() {
        String promptMessage = "**Step 4**\nChoose type of the event";
        Map<Integer,String> embedTypeMap = embedTypeService.getAllEmbedTypes()
                .stream()
                .collect(Collectors.toMap(EmbedType::getId, EmbedType::getName));
        CustomSelectMenu embedTypeCustomMenu = new CustomSelectMenu(
                "embed-type",
                "Choose type of the event!",
                embedTypeMap
        );
        MessageCreateSpec messageCreateSpec = getMessageCreateSpec(promptMessage, embedTypeCustomMenu);

        return new TextPromptMono.Builder<>(client, SelectMenuInteractionEvent.class)
                .withPromptType(TextPromptMono.PromptType.DEFAULT)
                .messageChannel(privateChannelMono)
                .messageCreateSpec(messageCreateSpec)
                .actionRowComponent(embedTypeCustomMenu)
                .withMessageCollector(messageCollector)
                .eventPredicate(promptFilter.selectInteractionEvent(embedTypeCustomMenu, user))
                .eventProcessor(processEmbedTypeInput())
                .build()
                .mono();
    }

    private Consumer<SelectMenuInteractionEvent> processEmbedTypeInput() {
        return event -> {
            String result = event.getValues().stream()
                    .findFirst()
                    .orElse("0");
            EmbedType embedType = embedTypeService.getEmbedTypeById(Integer.parseInt(result));
            eventBuilder.withEmbedType(embedType);
        };
    }

    public Mono<SelectMenuInteractionEvent> promptRaidSelect() {
        String promptMessage = "**Step 4**\nChoose which raids is this signup for:\nRequired 1 selection, maximum 3";
        RaidSelectMenu raidSelectMenu = new RaidSelectMenu();
        MessageCreateSpec prompt = getMessageCreateSpec(promptMessage, raidSelectMenu);

        return new TextPromptMono.Builder<>(client, SelectMenuInteractionEvent.class)
                .withPromptType(TextPromptMono.PromptType.DEFAULT)
                .messageCreateSpec(prompt)
                .actionRowComponent(raidSelectMenu)
                .messageChannel(privateChannelMono)
                .withMessageCollector(messageCollector)
                .eventPredicate(promptFilter.selectInteractionEvent(raidSelectMenu, user))
                .eventProcessor(processRaidSelectInput())
                .build()
                .mono();
    }

    private Consumer<SelectMenuInteractionEvent> processRaidSelectInput() {
        return event -> eventBuilder.withInstances(event.getValues());
    }

    public Mono<SelectMenuInteractionEvent> promptMemberSize() {
        MemberSizeSelectMenu memberSizeSelectMenu = new MemberSizeSelectMenu();
        String defaultSize = "25";
        String promptMessage = "**Step 5**\nChoose maximum attendants count for this event";
        MessageCreateSpec prompt = getMessageCreateSpec(promptMessage, memberSizeSelectMenu);

        return new TextPromptMono.Builder<>(client, SelectMenuInteractionEvent.class)
                .withPromptType(TextPromptMono.PromptType.DEFAULT)
                .messageChannel(privateChannelMono)
                .messageCreateSpec(prompt)
                .actionRowComponent(memberSizeSelectMenu)
                .withMessageCollector(messageCollector)
                .eventPredicate(promptFilter.selectInteractionEvent(memberSizeSelectMenu, user))
                .eventProcessor(processMemberSizeInput(defaultSize))
                .build()
                .mono();
    }

    private Consumer<SelectMenuInteractionEvent> processMemberSizeInput(String defaultSize) {
        return event -> {
            String result = event.getValues().stream().findFirst().orElse(defaultSize);
            eventBuilder.withMemberSize(result);
        };
    }

    public Mono<SelectMenuInteractionEvent> promptDestinationChannel() {
        String promptMessage = "**Step 6**\nChoose in which channel post this raid signup";
        Snowflake originChannelId = initialEvent.getInteraction().getChannelId();

        return promptService.fetchGuildTextChannels(initialEvent)
                .collectList()
                .flatMap(list -> {
                    ChannelSelectMenu channelSelectMenu = new ChannelSelectMenu(list);
                    MessageCreateSpec prompt = getMessageCreateSpec(promptMessage, channelSelectMenu);

                    return new TextPromptMono.Builder<>(client, SelectMenuInteractionEvent.class)
                            .withPromptType(TextPromptMono.PromptType.DEFAULT)
                            .messageChannel(privateChannelMono)
                            .messageCreateSpec(prompt)
                            .actionRowComponent(channelSelectMenu)
                            .withMessageCollector(messageCollector)
                            .eventPredicate(promptFilter.selectInteractionEvent(channelSelectMenu, user))
                            .eventProcessor(processDestinationChannelInput(originChannelId))
                            .build()
                            .mono();
                });
    }

    private Consumer<SelectMenuInteractionEvent> processDestinationChannelInput(Snowflake originChannelId) {
        return event -> {
            String result = event.getValues().stream()
                    .findFirst()
                    .orElse(originChannelId.asString());
            eventBuilder.withDestinationChannel(result);
        };
    }

    public Mono<ButtonInteractionEvent> promptConfirmationAndDeferReply() {
        ButtonRow buttonRow = ButtonRow.builder()
                .addButton("confirm", "Confirm", ButtonRow.Builder.ButtonType.PRIMARY)
                .addButton("cancel", "Cancel", ButtonRow.Builder.ButtonType.DANGER)
                .addButton("repeat", "Start Again!", ButtonRow.Builder.ButtonType.SECONDARY)
                .build();
        MessageCreateSpec prompt = MessageCreateSpec.builder()
                .addEmbed(embedGenerator.generatePreview(eventBuilder))
                .addComponent(buttonRow.getActionRow())
                .build();

        return new TextPromptMono.Builder<>(client, ButtonInteractionEvent.class)
                .withPromptType(TextPromptMono.PromptType.DEFERRABLE_REPLY)
                .messageChannel(privateChannelMono)
                .messageCreateSpec(prompt)
                .withMessageCollector(messageCollector)
                .eventPredicate(promptFilter.buttonInteractionEvent(buttonRow, user))
                .eventProcessor(event -> { })
                .build()
                .mono();
    }

    public Mono<Message> finalizeProcess() {
        Snowflake destinationChannel = Snowflake.of(eventBuilder.getDestinationChannelId());
        return initialEvent.getInteraction()
                .getGuild()
                .flatMap(guild -> guild.getChannelById(destinationChannel)
                        .cast(MessageChannel.class)
                        .flatMap(channel -> channel.createMessage("Generating event..."))
                        .flatMap(finalize(destinationChannel, guild))
                );
    }

    private Function<Message, Mono<? extends Message>> finalize(Snowflake destinationChannel, discord4j.core.object.entity.Guild guild) {
        return message -> {
            Snowflake messageId = message.getId();
            eventBuilder.withEventId(messageId.asString());
            Event event = buildEventAndSubscribeInteractions();
            String messageUrl = constructMessageUrl(destinationChannel, guild, messageId);
            Mono<Message> finalMessage = getFinalMessage("Event created in " + messageUrl);
            MessageEditSpec finalEmbed = getFinalEmbed(event);
            return message.edit(finalEmbed)
                    .then(finalMessage);
        };
    }

    private Event buildEventAndSubscribeInteractions() {
        Event event = eventBuilder.build();
        subscribeInteractionsAndSaveToDatabase(event);
        return event;
    }

    private void subscribeInteractionsAndSaveToDatabase(Event event) {
        embedGenerator.subscribeInteractions(event);
        eventService.saveEvent(event);
    }

    private MessageEditSpec getFinalEmbed(Event event) {
        return MessageEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(embedGenerator.generateEmbed(event))
                .addAllComponents(embedGenerator.generateComponents(event))
                .build();
    }

    private Mono<Message> getFinalMessage(String messageUrl) {
        return privateChannelMono
                .flatMap(channel -> channel.createMessage(messageUrl));
    }

    private String constructMessageUrl(Snowflake destinationChannel, discord4j.core.object.entity.Guild guild, Snowflake messageId) {
        return promptFormatter.messageUrl(guild.getId(), destinationChannel, messageId);
    }

    private static MessageCreateSpec getMessageCreateSpec(String message) {
        return MessageCreateSpec.builder()
                .content(message)
                .build();
    }

    private static MessageCreateSpec getMessageCreateSpec(String message, ActionRowComponent actionRowComponent) {
        return MessageCreateSpec.builder()
                .content(message)
                .addComponent(actionRowComponent.getActionRow())
                .build();
    }

    public void setPrivateChannel(Mono<PrivateChannel> privateChannel) {
        this.privateChannelMono = privateChannel;
    }

    public void setInitialEvent(ChatInputInteractionEvent initialEvent) {
        this.initialEvent = initialEvent;
    }

    public void setEventBuilder(Event.Builder builder) {
        this.eventBuilder = builder;
    }
}
