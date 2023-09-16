package com.github.havlli.EventPilot.prompt;

import com.github.havlli.EventPilot.component.ButtonRowComponent;
import com.github.havlli.EventPilot.component.SelectMenuComponent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
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
            Predicate<ComponentInteractionEvent> userPredicate = isUserInvolvedInInteraction(user);
            Predicate<ComponentInteractionEvent> componentPredicate = isComponentInvolvedInInteraction(selectMenuComponent);
            return userPredicate.and(componentPredicate).test(event);
        };
    }

    public Predicate<ButtonInteractionEvent> buttonInteractionEvent(ButtonRowComponent buttonRowComponent, User user) {
        return event -> {
            Predicate<ComponentInteractionEvent> userPredicate = isUserInvolvedInInteraction(user);
            Predicate<ComponentInteractionEvent> componentPredicate = isComponentInvolvedInInteraction(buttonRowComponent);
            return userPredicate.and(componentPredicate).test(event);
        };
    }

    public Predicate<MessageCreateEvent> isMessageAuthor(User user) {
        return event -> event.getMessage().getAuthor().equals(Optional.of(user));
    }

    private Predicate<ComponentInteractionEvent> isUserInvolvedInInteraction(User user) {
        return event -> event.getInteraction().getUser().equals(user);
    }

    private Predicate<ComponentInteractionEvent> isComponentInvolvedInInteraction(SelectMenuComponent selectMenuComponent) {
        return event -> event.getCustomId().equals(selectMenuComponent.getCustomId());
    }

    private Predicate<ComponentInteractionEvent> isComponentInvolvedInInteraction(ButtonRowComponent buttonRowComponent) {
        return event -> buttonRowComponent.getCustomIds()
                .stream()
                .anyMatch(customId -> customId.equals(event.getCustomId()));
    }
}
