package com.github.havlli.EventPilot.command.createevent;

import com.github.havlli.EventPilot.component.ButtonRow;
import com.github.havlli.EventPilot.component.SelectMenuComponent;
import com.github.havlli.EventPilot.component.selectmenu.ChannelSelectMenu;
import com.github.havlli.EventPilot.component.selectmenu.CustomSelectMenu;
import com.github.havlli.EventPilot.component.selectmenu.MemberSizeSelectMenu;
import com.github.havlli.EventPilot.core.GuildEventCreator;
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
    private final MessageCollector messageCollector;
    private final PromptFormatter promptFormatter;
    private final PromptService promptService;
    private final EmbedGenerator embedGenerator;
    private final EventService eventService;
    private final GuildService guildService;
    private final EmbedTypeService embedTypeService;
    private final TimeService timeService;
    private final GuildEventCreator guildEventCreator;
    private final TextPromptBuilderFactory promptBuilderFactory;
    private final ObjectFactory<CreateEventInteraction> provider;
    private ChatInputInteractionEvent initialEvent;
    private User user;
    private Mono<PrivateChannel> privateChannelMono;
    private Event.Builder eventBuilder;

    public CreateEventInteraction(
            MessageCollector messageCollector,
            PromptFormatter promptFormatter,
            PromptService promptService,
            EmbedGenerator embedGenerator,
            EventService eventService,
            GuildService guildService,
            EmbedTypeService embedTypeService,
            TimeService timeService,
            GuildEventCreator guildEventCreator,
            TextPromptBuilderFactory promptBuilderFactory,
            ObjectFactory<CreateEventInteraction> provider
    ) {
        this.messageCollector = messageCollector;
        this.promptFormatter = promptFormatter;
        this.promptService = promptService;
        this.embedGenerator = embedGenerator;
        this.eventService = eventService;
        this.guildService = guildService;
        this.embedTypeService = embedTypeService;
        this.timeService = timeService;
        this.guildEventCreator = guildEventCreator;
        this.promptBuilderFactory = promptBuilderFactory;
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
        String message = "**Step 1**\nEnter name for your event!";

        return createDeferMessageSpecMono(message)
                .flatMap(messageCreateSpec -> createMessagePrompt(messageCreateSpec, processNameInput()));
    }

    protected Mono<MessageCreateEvent> promptDescription() {
        String message = "**Step 2**\nEnter description";

        return createDeferMessageSpecMono(message)
                .flatMap(messageCreateSpec -> createMessagePrompt(messageCreateSpec, processDescriptionInput()));
    }

    protected Mono<MessageCreateEvent> promptDateTime() {
        String message = "**Step 3**\nEnter the date and time in UTC timezone (format: dd.MM.yyyy HH:mm)";

        return createDeferMessageSpecMono(message)
                .flatMap(messageCreateSpec -> createMessagePromptWithError(messageCreateSpec, processDateTimeInput()));
    }

    protected Mono<SelectMenuInteractionEvent> promptEmbedType() {
        String message = "**Step 4**\nChoose type of the event";

        return getAllEmbedTypesAndCreateCustomMenu()
                .flatMap(customMenu -> createSelectMenuPrompt(message, customMenu, processEmbedTypeInput()));
    }

    protected Mono<SelectMenuInteractionEvent> promptMemberSize() {
        MemberSizeSelectMenu memberSizeSelectMenu = new MemberSizeSelectMenu();
        String defaultSize = "25";
        String promptMessage = "**Step 5**\nChoose maximum attendants count for this event";

        return createSelectMenuPrompt(promptMessage, memberSizeSelectMenu, processMemberSizeInput(defaultSize));
    }

    protected Mono<SelectMenuInteractionEvent> promptDestinationChannel() {
        String promptMessage = "**Step 6**\nChoose in which channel post this raid signup";

        return getTextChannelsThenCreatePrompt(promptMessage);
    }

    protected Mono<ButtonInteractionEvent> promptConfirmationAndDeferReply() {
        ButtonRow buttonRow = createConfirmationButtonRow();
        return createConfirmationMessageSpec(buttonRow)
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
                    .then(repeatInteraction());
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

    private Mono<MessageCreateSpec> createDeferMessageSpecMono(String message) {
        return Mono.defer(() -> Mono.just(createMessageSpec(message)));
    }

    private static MessageCreateSpec createMessageSpec(String message) {
        return MessageCreateSpec.builder()
                .content(message)
                .build();
    }

    private Mono<MessageCreateEvent> createMessagePrompt(MessageCreateSpec messageCreateSpec, Consumer<MessageCreateEvent> eventProcessor) {
        return Mono.defer(() -> promptBuilderFactory.defaultPrivateMessageBuilder(initialEvent, messageCreateSpec)
                .withMessageCollector(messageCollector)
                .eventProcessor(eventProcessor)
                .build()
                .createMono());
    }

    private Mono<MessageCreateEvent> createMessagePromptWithError(MessageCreateSpec messageCreateSpec, Consumer<MessageCreateEvent> eventProcessor) {
        return Mono.defer(() -> promptBuilderFactory.defaultPrivateMessageBuilder(initialEvent, messageCreateSpec)
                .withMessageCollector(messageCollector)
                .eventProcessor(eventProcessor)
                .onErrorRepeat(DateTimeParseException.class, "Invalid Format")
                .build()
                .createMono()
                .onErrorResume(InvalidDateTimeException.class, sendMessageToUserAndRepeatOnException()));
    }

    private Mono<SelectMenuInteractionEvent> createSelectMenuPrompt(String prompt, SelectMenuComponent selectMenuComponent, Consumer<SelectMenuInteractionEvent> eventProcessor) {
        return Mono.defer(() -> promptBuilderFactory.defaultPrivateSelectMenuBuilder(initialEvent, prompt, selectMenuComponent)
                .withMessageCollector(messageCollector)
                .eventProcessor(eventProcessor)
                .build()
                .createMono());
    }

    private Mono<ButtonInteractionEvent> createDeferrableButtonPrompt(MessageCreateSpec prompt, ButtonRow buttonRow) {
        return Mono.defer(() -> promptBuilderFactory.deferrablePrivateButtonBuilder(initialEvent, prompt, buttonRow)
                .withMessageCollector(messageCollector)
                .build()
                .createMono());
    }

    private Mono<SelectMenuInteractionEvent> getTextChannelsThenCreatePrompt(String promptMessage) {
        return promptService.fetchGuildTextChannels(initialEvent)
                .collectList()
                .flatMap(textChannels -> {
                    ChannelSelectMenu channelSelectMenu = new ChannelSelectMenu(textChannels);

                    return createSelectMenuPrompt(promptMessage, channelSelectMenu, processDestinationChannelInput(getOriginChannelId()));
                });
    }

    private Snowflake getOriginChannelId() {
        return initialEvent.getInteraction().getChannelId();
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

    private Mono<CustomSelectMenu> getAllEmbedTypesAndCreateCustomMenu() {
        return Mono.defer(() -> getAllEmbedTypesMono()
                .flatMap(this::createCustomMenu));
    }

    private Mono<Map<Long, String>> getAllEmbedTypesMono() {
        Map<Long, String> embedTypeMap = embedTypeService.getAllEmbedTypes()
                .stream()
                .collect(Collectors.toMap(EmbedType::getId, EmbedType::getName));

        return Mono.just(embedTypeMap);

    }

    private Mono<CustomSelectMenu> createCustomMenu(Map<Long, String> embedTypeMap) {
        CustomSelectMenu embedTypeCustomMenu = new CustomSelectMenu(
                "embed-type",
                "Choose type of the event!",
                embedTypeMap
        );
        return Mono.just(embedTypeCustomMenu);
    }

    private Consumer<SelectMenuInteractionEvent> processEmbedTypeInput() {
        return event -> {
            String result = event.getValues().stream()
                    .findFirst()
                    .orElse("0");
            EmbedType embedType = embedTypeService.getEmbedTypeById(Long.parseLong(result));
            eventBuilder.withEmbedType(embedType);
        };
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

    private static ButtonRow createConfirmationButtonRow() {
        return ButtonRow.builder()
                .addButton("confirm", "Confirm", ButtonRow.Builder.ButtonType.PRIMARY)
                .addButton("cancel", "Cancel", ButtonRow.Builder.ButtonType.DANGER)
                .addButton("repeat", "Start Again!", ButtonRow.Builder.ButtonType.SECONDARY)
                .build();
    }

    private Mono<MessageCreateSpec> createConfirmationMessageSpec(ButtonRow buttonRow) {
        return Mono.fromSupplier(() -> MessageCreateSpec.builder()
                .addEmbed(embedGenerator.generatePreview(eventBuilder))
                .addComponent(buttonRow.getActionRow())
                .build());
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

    private Mono<Message> repeatInteraction() {
        return start(initialEvent);
    }

    private Function<InvalidDateTimeException, Mono<MessageCreateEvent>> sendMessageToUserAndRepeatOnException() {
        return e -> privateChannelMono
                .flatMap(channel -> channel.createMessage("Date and time of the event has to be in future!"))
                .flatMap(message -> {
                    messageCollector.collect(message);
                    return promptDateTime();
                });
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
                    .then(guildEventCreator.createScheduledEvent(event))
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

    protected void setPrivateChannel(Mono<PrivateChannel> privateChannel) {
        this.privateChannelMono = privateChannel;
    }

    protected void setInitialEvent(ChatInputInteractionEvent initialEvent) {
        this.initialEvent = initialEvent;
    }

    protected void setEventBuilder(Event.Builder builder) {
        this.eventBuilder = builder;
    }

    protected void setUser(User user) {
        this.user = user;
    }
}
