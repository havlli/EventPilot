package com.github.havlli.EventPilot.command.createevent;

import com.github.havlli.EventPilot.component.ActionRowComponent;
import com.github.havlli.EventPilot.component.ButtonRow;
import com.github.havlli.EventPilot.component.SelectMenuComponent;
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
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.annotation.Scope;
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
@Scope("prototype")
public class CreateEventInteraction {

    protected static Logger LOG = LoggerFactory.getLogger(CreateEventInteraction.class);
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
    private final ObjectFactory<CreateEventInteraction> provider;
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
            TimeService timeService,
            ObjectFactory<CreateEventInteraction> provider) {
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
        this.provider = provider;
    }

    public Mono<Message> initiateOn(ChatInputInteractionEvent event) {
        return createNewInstance().start(event);
    }

    public CreateEventInteraction createNewInstance() {
        return provider.getObject();
    }

    public Mono<Message> start(ChatInputInteractionEvent event) {
        initializeInteraction(event);

        return promptName()
                .flatMap(__ -> promptDescription())
                .flatMap(__ -> promptDateTime())
                .flatMap(__ -> promptEmbedType())
                .flatMap(__ -> promptRaidSelect())
                .flatMap(__ -> promptMemberSize())
                .flatMap(__ -> promptDestinationChannel())
                .flatMap(__ -> promptConfirmationAndDeferReply())
                .flatMap(this::handleConfirmationResponse)
                .doFinally(this::sequenceChecker)
                .then(terminateInteraction());
    }

    private void initializeInteraction(ChatInputInteractionEvent event) {
        this.initialEvent = event;
        this.user = initialEvent.getInteraction().getUser();
        this.privateChannelMono = user.getPrivateChannel();
        this.eventBuilder = initializeEventBuilder(initialEvent);
    }

    protected Mono<MessageCreateEvent> promptName() {
        String promptMessage = "**Step 1**\nEnter name for your event!";
        MessageCreateSpec messageCreateSpec = createMessageSpec(promptMessage);

        return createMessagePrompt(messageCreateSpec, processNameInput());
    }

    protected Mono<MessageCreateEvent> promptDescription() {
        String promptMessage = "**Step 2**\nEnter description";
        MessageCreateSpec messageCreateSpec = createMessageSpec(promptMessage);

        return createMessagePrompt(messageCreateSpec, processDescriptionInput());
    }

    protected Mono<MessageCreateEvent> promptDateTime() {
        String promptMessage = "**Step 3**\nEnter the date and time in UTC timezone (format: dd.MM.yyyy HH:mm)";
        MessageCreateSpec messageCreateSpec = createMessageSpec(promptMessage);

        return createMessagePromptWithError(messageCreateSpec, processDateTimeInput());
    }

    protected Mono<SelectMenuInteractionEvent> promptEmbedType() {
        String promptMessage = "**Step 4**\nChoose type of the event";
        Map<Integer, String> embedTypeMap = embedTypeService.getAllEmbedTypes()
                .stream()
                .collect(Collectors.toMap(EmbedType::getId, EmbedType::getName));
        CustomSelectMenu embedTypeCustomMenu = new CustomSelectMenu(
                "embed-type",
                "Choose type of the event!",
                embedTypeMap
        );
        MessageCreateSpec messageCreateSpec = createMessageSpec(promptMessage, embedTypeCustomMenu);

        return createSelectMenuPrompt(messageCreateSpec, embedTypeCustomMenu, processEmbedTypeInput());
    }

    protected Mono<SelectMenuInteractionEvent> promptRaidSelect() {
        String promptMessage = "**Step 4**\nChoose which raids is this signup for:\nRequired 1 selection, maximum 3";
        RaidSelectMenu raidSelectMenu = new RaidSelectMenu();
        MessageCreateSpec prompt = createMessageSpec(promptMessage, raidSelectMenu);

        return createSelectMenuPrompt(prompt, raidSelectMenu, processRaidSelectInput());
    }

    protected Mono<SelectMenuInteractionEvent> promptMemberSize() {
        MemberSizeSelectMenu memberSizeSelectMenu = new MemberSizeSelectMenu();
        String defaultSize = "25";
        String promptMessage = "**Step 5**\nChoose maximum attendants count for this event";
        MessageCreateSpec prompt = createMessageSpec(promptMessage, memberSizeSelectMenu);

        return createSelectMenuPrompt(prompt, memberSizeSelectMenu, processMemberSizeInput(defaultSize));
    }

    protected Mono<SelectMenuInteractionEvent> promptDestinationChannel() {
        String promptMessage = "**Step 6**\nChoose in which channel post this raid signup";
        Snowflake originChannelId = initialEvent.getInteraction().getChannelId();

        return getTextChannelsThenCreatePrompt(promptMessage, originChannelId);
    }

    protected Mono<ButtonInteractionEvent> promptConfirmationAndDeferReply() {
        ButtonRow buttonRow = createConfirmationButtonRow();
        return createConfirmationPrompt(buttonRow)
                .flatMap(prompt -> createDeferrableButtonPrompt(prompt, buttonRow));
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

    protected void sequenceChecker(SignalType signalType) {
        switch (signalType) {
            case ON_COMPLETE -> LOG.info("Sequence completed successfully");
            case ON_ERROR -> LOG.info("Sequence completed with an error");
        }
    }

    private static Mono<Message> terminateInteraction() {
        return Mono.empty();
    }

    private Event.Builder initializeEventBuilder(ChatInputInteractionEvent initialEvent) {
        Snowflake guildId = promptService.fetchGuildId(initialEvent);
        Guild guild = guildService.getGuildById(guildId.asString());

        return Event.builder()
                .withAuthor(user.getUsername())
                .withGuild(guild);
    }

    private static MessageCreateSpec createMessageSpec(String message) {
        return MessageCreateSpec.builder()
                .content(message)
                .build();
    }

    private static MessageCreateSpec createMessageSpec(String message, ActionRowComponent actionRowComponent) {
        return createMessageSpec(message)
                .withComponents(actionRowComponent.getActionRow());
    }

    private Mono<MessageCreateEvent> createMessagePrompt(MessageCreateSpec messageCreateSpec, Consumer<MessageCreateEvent> eventProcessor) {
        return new TextPromptBuilder.Builder<>(client, MessageCreateEvent.class)
                .withPromptType(TextPromptBuilder.PromptType.DEFAULT)
                .messageChannel(privateChannelMono)
                .messageCreateSpec(messageCreateSpec)
                .withMessageCollector(messageCollector)
                .eventPredicate(promptFilter.isMessageAuthor(user))
                .eventProcessor(eventProcessor)
                .build()
                .createMono();
    }

    private Mono<MessageCreateEvent> createMessagePromptWithError(MessageCreateSpec messageCreateSpec, Consumer<MessageCreateEvent> eventProcessor) {
        return new TextPromptBuilder.Builder<>(client, MessageCreateEvent.class)
                .withPromptType(TextPromptBuilder.PromptType.DEFAULT)
                .messageChannel(privateChannelMono)
                .messageCreateSpec(messageCreateSpec)
                .withMessageCollector(messageCollector)
                .eventPredicate(promptFilter.isMessageAuthor(user))
                .eventProcessor(eventProcessor)
                .onErrorRepeat(DateTimeParseException.class, "Invalid Format")
                .build()
                .createMono()
                .onErrorResume(InvalidDateTimeException.class, sendMessageToUserAndRepeatOnException());
    }

    private Mono<SelectMenuInteractionEvent> createSelectMenuPrompt(MessageCreateSpec messageCreateSpec, SelectMenuComponent selectMenuComponent, Consumer<SelectMenuInteractionEvent> eventProcessor) {
        return new TextPromptBuilder.Builder<>(client, SelectMenuInteractionEvent.class)
                .withPromptType(TextPromptBuilder.PromptType.DEFAULT)
                .messageChannel(privateChannelMono)
                .messageCreateSpec(messageCreateSpec)
                .actionRowComponent(selectMenuComponent)
                .withMessageCollector(messageCollector)
                .eventPredicate(promptFilter.selectInteractionEvent(selectMenuComponent, user))
                .eventProcessor(eventProcessor)
                .build()
                .createMono();
    }

    private Mono<SelectMenuInteractionEvent> getTextChannelsThenCreatePrompt(String promptMessage, Snowflake originChannelId) {
        return promptService.fetchGuildTextChannels(initialEvent)
                .collectList()
                .flatMap(textChannels -> {
                    ChannelSelectMenu channelSelectMenu = new ChannelSelectMenu(textChannels);
                    MessageCreateSpec promptSpec = createMessageSpec(promptMessage, channelSelectMenu);

                    return createSelectMenuPrompt(promptSpec, channelSelectMenu, processDestinationChannelInput(originChannelId));
                });
    }

    private static ButtonRow createConfirmationButtonRow() {
        return ButtonRow.builder()
                .addButton("confirm", "Confirm", ButtonRow.Builder.ButtonType.PRIMARY)
                .addButton("cancel", "Cancel", ButtonRow.Builder.ButtonType.DANGER)
                .addButton("repeat", "Start Again!", ButtonRow.Builder.ButtonType.SECONDARY)
                .build();
    }

    private Mono<MessageCreateSpec> createConfirmationPrompt(ButtonRow buttonRow) {
        return Mono.fromSupplier(() -> MessageCreateSpec.builder()
                .addEmbed(embedGenerator.generatePreview(eventBuilder))
                .addComponent(buttonRow.getActionRow())
                .build());
    }

    private Mono<ButtonInteractionEvent> createDeferrableButtonPrompt(MessageCreateSpec prompt, ButtonRow buttonRow) {
        return new TextPromptBuilder.Builder<>(client, ButtonInteractionEvent.class)
                .withPromptType(TextPromptBuilder.PromptType.DEFERRABLE_REPLY)
                .messageChannel(privateChannelMono)
                .messageCreateSpec(prompt)
                .withMessageCollector(messageCollector)
                .eventPredicate(promptFilter.buttonInteractionEvent(buttonRow, user))
                .eventProcessor(event -> {})
                .build()
                .createMono();
    }

    public Mono<Message> finalizeProcess() {
        Snowflake destinationChannel = Snowflake.of(eventBuilder.getDestinationChannelId());
        LOG.info("finalizeProcess - Destination channel: {}", destinationChannel);
        return initialEvent.getInteraction()
                .getGuild()
                .flatMap(guild -> guild.getChannelById(destinationChannel)
                        .cast(MessageChannel.class)
                        .flatMap(channel -> channel.createMessage("Generating event..."))
                        .flatMap(finalize(destinationChannel, guild))
                );
    }

    private Flux<Void> deleteAllSentMessages() {
        return messageCollector.cleanup();
    }

    private Mono<Void> sendCompleteDeferredInteractionSignal(ButtonInteractionEvent event) {
        return event.getInteractionResponse().deleteInitialResponse();
    }

    private Mono<Message> startOver() {
        return start(initialEvent);
    }

    private Consumer<MessageCreateEvent> processNameInput() {
        return event -> eventBuilder.withName(event.getMessage().getContent());
    }

    private Consumer<MessageCreateEvent> processDescriptionInput() {
        return event -> eventBuilder.withDescription(event.getMessage().getContent());
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
                    messageCollector.collect(message);
                    return promptDateTime();
                });
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

    private Consumer<SelectMenuInteractionEvent> processRaidSelectInput() {
        return event -> eventBuilder.withInstances(event.getValues());
    }

    private Consumer<SelectMenuInteractionEvent> processMemberSizeInput(String defaultSize) {
        return event -> {
            String result = event.getValues().stream().findFirst().orElse(defaultSize);
            eventBuilder.withMemberSize(result);
        };
    }

    private Consumer<SelectMenuInteractionEvent> processDestinationChannelInput(Snowflake originChannelId) {
        return event -> {
            String result = event.getValues().stream()
                    .findFirst()
                    .orElse(originChannelId.asString());
            eventBuilder.withDestinationChannel(result);
        };
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

    private String constructMessageUrl(Snowflake destinationChannel, discord4j.core.object.entity.Guild guild, Snowflake messageId) {
        return promptFormatter.messageUrl(guild.getId(), destinationChannel, messageId);
    }

    private Mono<Message> getFinalMessage(String messageUrl) {
        return privateChannelMono
                .flatMap(channel -> channel.createMessage(messageUrl));
    }

    private MessageEditSpec getFinalEmbed(Event event) {
        return MessageEditSpec.builder()
                .contentOrNull(null)
                .addEmbed(embedGenerator.generateEmbed(event))
                .addAllComponents(embedGenerator.generateComponents(event))
                .build();
    }

    private void subscribeInteractionsAndSaveToDatabase(Event event) {
        embedGenerator.subscribeInteractions(event);
        eventService.saveEvent(event);
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
