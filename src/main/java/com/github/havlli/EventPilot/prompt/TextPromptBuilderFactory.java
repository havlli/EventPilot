package com.github.havlli.EventPilot.prompt;

import com.github.havlli.EventPilot.component.ButtonRowComponent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import org.springframework.stereotype.Component;

@Component
public class TextPromptBuilderFactory {

    private final PromptFilter promptFilter;

    public TextPromptBuilderFactory(PromptFilter promptFilter) {
        this.promptFilter = promptFilter;
    }

    public TextPromptBuilder.Builder<MessageCreateEvent> defaultPrivateMessageBuilder(ChatInputInteractionEvent event, String promptMessage) {
        User user = event.getInteraction().getUser();
        return new TextPromptBuilder.Builder<>(event.getClient(), MessageCreateEvent.class)
                .withPromptType(TextPromptBuilder.PromptType.DEFAULT)
                .messageChannel(user.getPrivateChannel())
                .messageCreateSpec(MessageCreateSpec.builder().content(promptMessage).build())
                .eventPredicate(promptFilter.isMessageAuthor(user));
    }

    public TextPromptBuilder.Builder<ButtonInteractionEvent> deferrablePrivateButtonBuilder(
            ChatInputInteractionEvent event,
            String promptMessage,
            EmbedCreateSpec embed,
            ButtonRowComponent buttonRowComponent
    ) {
        User user = event.getInteraction().getUser();
        return new TextPromptBuilder.Builder<>(event.getClient(), ButtonInteractionEvent.class)
                .withPromptType(TextPromptBuilder.PromptType.DEFERRABLE_REPLY)
                .messageChannel(user.getPrivateChannel())
                .actionRowComponent(buttonRowComponent)
                .messageCreateSpec(MessageCreateSpec.builder()
                        .content(promptMessage)
                        .addComponent(buttonRowComponent.getActionRow())
                        .addEmbed(embed)
                        .build())
                .eventPredicate(promptFilter.buttonInteractionEvent(buttonRowComponent, user))
                .eventProcessor(e -> {});
    }
}
