package com.github.havlli.EventPilot.command.createembedtype;

import com.github.havlli.EventPilot.component.ButtonRowComponent;
import com.github.havlli.EventPilot.component.CustomComponentFactory;
import com.github.havlli.EventPilot.entity.embedtype.EmbedType;
import com.github.havlli.EventPilot.entity.embedtype.EmbedTypeService;
import com.github.havlli.EventPilot.generator.EmbedGenerator;
import com.github.havlli.EventPilot.prompt.MessageCollector;
import com.github.havlli.EventPilot.prompt.TextPromptBuilderFactory;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Locale;

@Component
@Scope("prototype")
public class CreateEmbedTypeInteraction {

    private final ObjectFactory<CreateEmbedTypeInteraction> objectFactory;
    private final MessageCollector messageCollector;
    private final EmbedTypeService embedTypeService;
    private final EmbedGenerator embedGenerator;
    private final TextPromptBuilderFactory promptBuilderFactory;
    private final CustomComponentFactory componentFactory;
    private final MessageSource messageSource;
    private ChatInputInteractionEvent initialEvent;
    private User user;
    private EmbedType.Builder embedTypeBuilder;

    public CreateEmbedTypeInteraction(
            ObjectFactory<CreateEmbedTypeInteraction> objectFactory,
            MessageCollector messageCollector,
            EmbedTypeService embedTypeService,
            EmbedGenerator embedGenerator,
            TextPromptBuilderFactory promptBuilderFactory,
            CustomComponentFactory componentFactory,
            MessageSource messageSource
    ) {
        this.objectFactory = objectFactory;
        this.messageCollector = messageCollector;
        this.embedTypeService = embedTypeService;
        this.embedGenerator = embedGenerator;
        this.promptBuilderFactory = promptBuilderFactory;
        this.componentFactory = componentFactory;
        this.messageSource = messageSource;
    }

    public Mono<Message> initiateOn(ChatInputInteractionEvent event) {
        return createNewInstance().startInteraction(event);
    }

    private CreateEmbedTypeInteraction createNewInstance() {
        return objectFactory.getObject();
    }

    protected Mono<Message> startInteraction(ChatInputInteractionEvent event) {
        initializeInteraction(event);

        return promptName()
                .flatMap(__ -> promptImportDialog())
                .flatMap(__ -> promptConfirmation())
                .flatMap(this::handleConfirmationResponse)
                .then(terminateInteraction());
    }

    protected void initializeInteraction(ChatInputInteractionEvent event) {
        this.initialEvent = event;
        this.user = event.getInteraction().getUser();
        this.embedTypeBuilder = EmbedType.builder();
    }

    protected Mono<MessageCreateEvent> promptName() {
        String prompt = messageSource.getMessage("interaction.create-embed-type.name", null, Locale.ENGLISH);

        return Mono.defer(() -> promptBuilderFactory.defaultPrivateMessageBuilder(initialEvent, prompt)
                .withMessageCollector(messageCollector)
                .eventProcessor(event -> embedTypeBuilder.withName(event.getMessage().getContent()))
                .build()
                .createMono());
    }

    protected Mono<MessageCreateEvent> promptImportDialog() {
        String prompt = messageSource.getMessage("interaction.create-embed-type.import-json", null, Locale.ENGLISH);
        String errorMessage = messageSource.getMessage("interaction.create-embed-type.import-json.exception", null, Locale.ENGLISH);

        return Mono.defer(() -> promptBuilderFactory.defaultPrivateMessageBuilder(initialEvent, prompt)
                .withMessageCollector(messageCollector)
                .eventProcessor(this::processImportWithException)
                .onErrorRepeat(RuntimeException.class, errorMessage)
                .build()
                .createMono());
    }

    protected Mono<ButtonInteractionEvent> promptConfirmation() {
        String prompt = messageSource.getMessage("interaction.create-embed-type.confirmation", null, Locale.ENGLISH);

        ButtonRowComponent buttonRow = componentFactory.getConfirmationButtonRow();
        return Mono.defer(() -> promptBuilderFactory.deferrablePrivateButtonBuilder(initialEvent, prompt, generatePreview(), buttonRow)
                .withMessageCollector(messageCollector)
                .build()
                .createMono());
    }

    protected Mono<?> handleConfirmationResponse(ButtonInteractionEvent event) {
        String customId = event.getCustomId();
        return switch (customId) {
            case "confirm" -> finalizeProcess()
                    .thenMany(deleteAllPromptedMessages())
                    .then(sendCompleteDeferredInteractionSignal(event));
            case "repeat" -> deleteAllPromptedMessages()
                    .then(sendCompleteDeferredInteractionSignal(event))
                    .then(startInteraction(initialEvent));
            default -> deleteAllPromptedMessages()
                    .then(sendCompleteDeferredInteractionSignal(event));
        };
    }

    private static Mono<Message> terminateInteraction() {
        return Mono.empty();
    }

    protected void processImportWithException(MessageCreateEvent messageCreateEvent) {
        String content = messageCreateEvent.getMessage().getContent();
        String json = embedTypeService.validateJsonOrThrow(content, new RuntimeException("JsonProcessingException"));
        embedTypeBuilder.withStructure(json);
    }

    private EmbedCreateSpec generatePreview() {
        EmbedType embedType = embedTypeBuilder.build();

        return embedGenerator.generateEmbedTypePreview(embedType);
    }

    protected Mono<Message> finalizeProcess() {
        return deferredSaveToDatabase()
                .flatMap(this::sendCompleteMessage);
    }

    private Flux<Void> deleteAllPromptedMessages() {
        return messageCollector.cleanup();
    }

    protected Mono<Void> sendCompleteDeferredInteractionSignal(ButtonInteractionEvent event) {
        return event.getInteractionResponse().deleteInitialResponse();
    }

    private Mono<EmbedType> deferredSaveToDatabase() {
        return Mono.defer(() -> {
            EmbedType embedType = embedTypeBuilder.build();
            embedTypeService.saveEmbedType(embedType);
            return Mono.just(embedType);
        });
    }

    private Mono<Message> sendCompleteMessage(EmbedType embedType) {
        String message = messageSource.getMessage("interaction.create-embed-type.complete", new Object[]{embedType.getName()}, Locale.ENGLISH);
        return user.getPrivateChannel()
                .flatMap(channel -> channel.createMessage(MessageCreateSpec.builder()
                        .content(message)
                        .build())
                );
    }

    protected void setEmbedTypeBuilder(EmbedType.Builder builder) {
        this.embedTypeBuilder = builder;
    }

    protected void setInitialEvent(ChatInputInteractionEvent initialEvent) {
        this.initialEvent = initialEvent;
    }

    protected void setUser(User user) {
        this.user = user;
    }
}
