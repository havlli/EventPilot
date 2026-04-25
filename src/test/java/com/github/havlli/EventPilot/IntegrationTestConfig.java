package com.github.havlli.EventPilot;

import com.github.havlli.EventPilot.core.DiscordService;
import discord4j.core.GatewayDiscordClient;
import discord4j.rest.RestClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class IntegrationTestConfig {

    @Bean
    public GatewayDiscordClient gatewayDiscordClient() {
        GatewayDiscordClient gatewayDiscordClient = mock(GatewayDiscordClient.class);
        when(gatewayDiscordClient.getMessageById(any(), any())).thenReturn(Mono.empty());
        return gatewayDiscordClient;
    }

    @Bean
    public RestClient restClient() {
        return mock(RestClient.class);
    }

    @Bean
    @Primary
    public DiscordService discordService() {
        DiscordService discordService = mock(DiscordService.class);
        when(discordService.updateEventMessage(any())).thenReturn(Mono.empty());
        when(discordService.deleteEventMessage(any())).thenReturn(Mono.empty());
        when(discordService.deactivateEvents(any())).thenReturn(Flux.empty());
        return discordService;
    }
}
