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
        commands.sort(typeComparator);
        return Flux.fromIterable(commands)
                .flatMap(slashCommand -> client.on(slashCommand.getEventType(), slashCommand::handle));
    }
}
