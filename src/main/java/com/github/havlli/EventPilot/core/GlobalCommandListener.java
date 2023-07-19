package com.github.havlli.EventPilot.core;

import com.github.havlli.EventPilot.command.SlashCommand;
import discord4j.core.GatewayDiscordClient;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.List;

@Component
@DependsOn({"restClient"})
public class GlobalCommandListener {

    private final Collection<SlashCommand> commands;
    private final GatewayDiscordClient client;

    public GlobalCommandListener(List<SlashCommand> slashCommands, GatewayDiscordClient client) {
        this.commands = slashCommands;
        this.client = client;
    }

    @PostConstruct
    private void registerListeners() {
        constructListeners().subscribe();
    }

    public Flux<?> constructListeners() {
        Flux<?> commandFlux = Flux.empty();

        for (SlashCommand command : commands) {
            Flux<?> flux = client.on(command.getEventType(), command::handle);
            commandFlux = Flux.merge(commandFlux, flux);
        }

        return commandFlux;
    }
}
