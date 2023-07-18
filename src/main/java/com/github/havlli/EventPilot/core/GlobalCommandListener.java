package com.github.havlli.EventPilot.core;

import com.github.havlli.EventPilot.command.SlashCommand;
import discord4j.core.GatewayDiscordClient;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

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
        registerListeners();
    }

    private void registerListeners() {
        commands.forEach(command -> client
                .on(command.getEventType(), command::handle)
                .subscribe()
        );
    }
}
