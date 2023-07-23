package com.github.havlli.EventPilot.prompt;

import com.github.havlli.EventPilot.component.ButtonRowComponent;
import com.github.havlli.EventPilot.component.SelectMenuComponent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Predicate;

@Component
public class PromptFilter {

    public Predicate<SelectMenuInteractionEvent> selectInteractionEvent(SelectMenuComponent selectMenuComponent, User user) {
        return event -> {
            boolean isSameUser = event.getInteraction().getUser().equals(user);
            boolean isComponent = event.getCustomId().equals(selectMenuComponent.getCustomId());
            return isSameUser && isComponent;
        };
    }

    public Predicate<ButtonInteractionEvent> buttonInteractionEvent(ButtonRowComponent buttonRowComponent, User user) {
        return event -> {
            boolean isSameUser = event.getInteraction().getUser().equals(user);
            boolean hasComponents = buttonRowComponent.getCustomIds()
                    .stream()
                    .anyMatch(customId -> customId.equals(event.getCustomId()));
            return isSameUser && hasComponents;
        };
    }

    public Predicate<MessageCreateEvent> isMessageAuthor(User user) {
        return event -> event.getMessage().getAuthor().equals(Optional.of(user));
    }
}
