package com.github.havlli.EventPilot.command;

import com.github.havlli.EventPilot.component.ButtonRow;
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
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Scope("prototype")
public class CreateEmbedTypeInteraction {

    private final ObjectFactory<CreateEmbedTypeInteraction> objectFactory;
    private final MessageCollector messageCollector;
    private final EmbedTypeService embedTypeService;
    private final EmbedGenerator embedGenerator;
    private final TextPromptBuilderFactory promptBuilderFactory;
    private ChatInputInteractionEvent initialEvent;
    private User user;
    private EmbedType.Builder embedTypeBuilder;

    public CreateEmbedTypeInteraction(
            ObjectFactory<CreateEmbedTypeInteraction> objectFactory,
            MessageCollector messageCollector,
            EmbedTypeService embedTypeService,
            EmbedGenerator embedGenerator,
            TextPromptBuilderFactory promptBuilderFactory
    ) {
        this.objectFactory = objectFactory;
        this.messageCollector = messageCollector;
        this.embedTypeService = embedTypeService;
        this.embedGenerator = embedGenerator;
        this.promptBuilderFactory = promptBuilderFactory;
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
        String prompt = "**Step 1**\nEnter name for your embed type!";

        return Mono.defer(() -> promptBuilderFactory.defaultPrivateMessageBuilder(initialEvent, prompt)
                .withMessageCollector(messageCollector)
                .eventProcessor(event -> embedTypeBuilder.withName(event.getMessage().getContent()))
                .build()
                .createMono());
    }

    protected Mono<MessageCreateEvent> promptImportDialog() {
        String prompt = """
                **Step 2**
                Please provide a JSON object that follows the following structure:
                ```json
                {
                  "-1": "Absence",
                  "-2": "Late",
                  "1": "Tank",
                  "-3": "Tentative",
                  "2": "Melee",
                  "3": "Ranged",
                  "4": "Healer",
                  "5": "Support"
                }
                ```
                - The JSON object should be enclosed in curly braces {}.
                - Each key-value pair should be separated by a colon :.
                - The keys should be integers.
                - The values should be strings.
                - The keys and values should be enclosed in double quotes "".
                - The keys can be positive or negative integers.
                - The values should correspond to the provided descriptions.
                - Negative keys indicate that the field should be displayed inline.
                - Positive keys indicate that the field should not be displayed inline.
                                
                Please make sure your JSON object adheres to these rules. If you have any questions, feel free to ask.
                """;

        return Mono.defer(() -> promptBuilderFactory.defaultPrivateMessageBuilder(initialEvent, prompt)
                .withMessageCollector(messageCollector)
                .eventProcessor(this::processImportWithException)
                .onErrorRepeat(RuntimeException.class, "JsonProcessingException")
                .build()
                .createMono());
    }

    protected Mono<ButtonInteractionEvent> promptConfirmation() {
        String prompt = "Are you sure you want to create this embed type?";
        ButtonRow buttonRow = ButtonRow.builder()
                .addButton("confirm", "Confirm", ButtonRow.Builder.ButtonType.PRIMARY)
                .addButton("cancel", "Cancel", ButtonRow.Builder.ButtonType.DANGER)
                .addButton("repeat", "Start Again!", ButtonRow.Builder.ButtonType.SECONDARY)
                .build();
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
        String prompt = "Your EmbedType **%s** was successfully created!".formatted(embedType.getName());
        return user.getPrivateChannel()
                .flatMap(channel -> channel.createMessage(MessageCreateSpec.builder()
                        .content(prompt)
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
