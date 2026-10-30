package com.github.havlli.EventPilot.health;

import discord4j.core.GatewayDiscordClient;
import discord4j.gateway.GatewayClient;
import discord4j.gateway.GatewayClientGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class HealthGatewayCheckTest {
    private GatewayClientGroup group;
    private GatewayClient firstShard;
    private GatewayClient secondShard;
    private DiscordGatewayHealthCheck check;

    @BeforeEach
    void setUp() {
        GatewayDiscordClient gateway = mock(GatewayDiscordClient.class);
        group = mock(GatewayClientGroup.class);
        firstShard = mock(GatewayClient.class);
        secondShard = mock(GatewayClient.class);
        when(gateway.getGatewayClientGroup()).thenReturn(group);
        when(group.getShardCount()).thenReturn(2);
        when(group.find(0)).thenReturn(Optional.of(firstShard));
        when(group.find(1)).thenReturn(Optional.of(secondShard));
        when(firstShard.isConnected()).thenReturn(Mono.just(true));
        when(secondShard.isConnected()).thenReturn(Mono.just(true));
        check = new DiscordGatewayHealthCheck(gateway);
    }

    @Test
    void requiresAllShardsToBeConnectedAndReadsCurrentStateOnEachCheck() {
        assertThat(check.isConnected()).isTrue();
        when(secondShard.isConnected()).thenReturn(Mono.just(false));
        assertThat(check.isConnected()).isFalse();
        when(secondShard.isConnected()).thenReturn(Mono.just(true));
        assertThat(check.isConnected()).isTrue();
    }

    @Test
    void missingShardIsDown() {
        when(group.find(1)).thenReturn(Optional.empty());
        assertThat(check.isConnected()).isFalse();
    }

    @Test
    void emptyConnectionStateIsDown() {
        when(secondShard.isConnected()).thenReturn(Mono.empty());
        assertThat(check.isConnected()).isFalse();
    }

    @Test
    void noShardsIsDown() {
        when(group.getShardCount()).thenReturn(0);
        assertThat(check.isConnected()).isFalse();
    }

    @Test
    void unansweredConnectionStateTimesOutAndCancelsSubscription() {
        boolean[] cancelled = {false};
        when(secondShard.isConnected()).thenReturn(Mono.<Boolean>never()
                .doOnCancel(() -> cancelled[0] = true));

        assertThatThrownBy(check::isConnected).isInstanceOf(IllegalStateException.class);
        assertThat(cancelled[0]).isTrue();
    }
}
