package com.github.havlli.EventPilot.health;

import discord4j.core.GatewayDiscordClient;
import discord4j.gateway.GatewayClient;
import discord4j.gateway.GatewayClientGroup;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@Profile("!test")
@ConditionalOnProperty(prefix = "eventpilot.health", name = "enabled", havingValue = "true")
public class DiscordGatewayHealthCheck implements GatewayHealthCheck {
    private final GatewayDiscordClient gateway;

    public DiscordGatewayHealthCheck(GatewayDiscordClient gateway) {
        this.gateway = gateway;
    }

    @Override
    public boolean isConnected() {
        GatewayClientGroup group = gateway.getGatewayClientGroup();
        int shardCount = group.getShardCount();
        if (shardCount < 1) {
            return false;
        }

        // Discord4J 3.3.2 exposes the current CONNECTED state on each shard, not on the group.
        // Require every shard; a missing shard or an unanswered state is not healthy.
        return Boolean.TRUE.equals(Flux.range(0, shardCount)
                .flatMap(shard -> group.find(shard)
                        .map(GatewayClient::isConnected)
                        .orElseGet(() -> Mono.just(false))
                        .defaultIfEmpty(false))
                .all(Boolean.TRUE::equals)
                .block(Duration.ofSeconds(2)));
    }
}
