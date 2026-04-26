package com.github.havlli.EventPilot.core;

import com.github.havlli.EventPilot.command.SlashCommand;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;

@Component
@Profile("!test")
@DependsOn({"restClient"})
public class GlobalCommandListener {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalCommandListener.class);
    private final List<SlashCommand> commands;
    private final GatewayDiscordClient client;
    private final Comparator<SlashCommand> typeComparator;

    public GlobalCommandListener(
            List<SlashCommand> slashCommands,
            GatewayDiscordClient client,
            Comparator<SlashCommand> typeComparator
    ) {
        this.commands = slashCommands;
        this.client = client;
        this.typeComparator = typeComparator;
    }

    @PostConstruct
    private void registerListeners() {
        createListeners().subscribe(
                __ -> {
                },
                error -> LOG.error("Discord listener registration failed", error)
        );
    }

    public Flux<?> createListeners() {
        commands.sort(typeComparator);
        return Flux.fromIterable(commands)
                .flatMap(this::createListener);
    }

    private Flux<?> createListener(SlashCommand command) {
        return client.on(command.getEventType(), event -> handleCommand(command, event))
                .onErrorResume(error -> {
                    LOG.error("Discord listener failed for command [{}]", command.getName(), error);
                    return Flux.empty();
                });
    }

    private Mono<?> handleCommand(SlashCommand command, Event event) {
        try {
            return command.handle(event)
                    .doOnError(error -> LOG.error("Discord command [{}] failed", command.getName(), error))
                    .onErrorResume(error -> Mono.empty());
        } catch (RuntimeException error) {
            LOG.error("Discord command [{}] failed before returning a response", command.getName(), error);
            return Mono.empty();
        }
    }
}
