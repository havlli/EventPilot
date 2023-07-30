package com.github.havlli.EventPilot.core;

import com.github.havlli.EventPilot.command.SlashCommand;
import discord4j.core.GatewayDiscordClient;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Comparator;
import java.util.List;

@Component
@DependsOn({"restClient"})
public class GlobalCommandListener {

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
        constructListeners().subscribe();
    }

    public Flux<?> constructListeners() {
        Flux<?> commandFlux = Flux.empty();

        // Sorting to make sure that OnReadyEvent is the first one to get merged to Flux hence first one to get subscribed
        commands.sort(typeComparator);

        for (SlashCommand command : commands) {
            Flux<?> flux = client.on(command.getEventType(), command::handle);
            commandFlux = Flux.merge(commandFlux, flux);
        }

        return commandFlux;
    }
}
