package com.github.havlli.EventPilot.core;

import discord4j.core.GatewayDiscordClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GatewayKeepAliveTest {

    private GatewayDiscordClient gateway;
    private GatewayKeepAlive underTest;
    private Sinks.Empty<Void> disconnected;
    private CountDownLatch subscribed;
    private AtomicReference<Thread> waiter;

    @BeforeEach
    void setUp() {
        gateway = mock(GatewayDiscordClient.class);
        disconnected = Sinks.empty();
        subscribed = new CountDownLatch(1);
        waiter = new AtomicReference<>();
        when(gateway.onDisconnect()).thenReturn(disconnected.asMono().doOnSubscribe(subscription -> {
            waiter.set(Thread.currentThread());
            subscribed.countDown();
        }));
        when(gateway.logout()).thenReturn(Mono.fromRunnable(() -> disconnected.tryEmitEmpty()));
        underTest = new GatewayKeepAlive(gateway);
    }

    @AfterEach
    void tearDown() {
        underTest.stop();
    }

    @Test
    void startLaunchesNonDaemonThreadWithoutBlockingCaller() throws InterruptedException {
        underTest.start();
        awaitSubscription();

        assertThat(waiter.get()).isNotSameAs(Thread.currentThread());
        assertThat(waiter.get().isDaemon()).isFalse();
        assertThat(waiter.get().isAlive()).isTrue();
        assertThat(underTest.isRunning()).isTrue();
    }

    @Test
    void threadEndsWhenOnDisconnectCompletes() throws InterruptedException {
        underTest.start();
        awaitSubscription();

        disconnected.tryEmitEmpty();
        waiter.get().join(2000);

        assertThat(waiter.get().isAlive()).isFalse();
        assertThat(underTest.isRunning()).isFalse();
        verify(gateway, never()).logout();
    }

    @Test
    void stopLogsOutOnceAndIsIdempotent() throws InterruptedException {
        underTest.start();
        awaitSubscription();

        underTest.stop();
        underTest.stop();

        verify(gateway, times(1)).logout();
        assertThat(waiter.get().isAlive()).isFalse();
        assertThat(underTest.isRunning()).isFalse();
    }

    @Test
    void repeatedStartUsesOneWaiter() throws InterruptedException {
        underTest.start();
        awaitSubscription();
        underTest.start();

        verify(gateway, times(1)).onDisconnect();
    }

    @Test
    void stopReleasesWaiterIfLogoutFails() throws InterruptedException {
        when(gateway.logout()).thenReturn(Mono.error(new IllegalStateException("Logout failed")));
        underTest.start();
        awaitSubscription();

        underTest.stop();
        underTest.stop();

        verify(gateway, times(1)).logout();
        assertThat(waiter.get().isAlive()).isFalse();
        assertThat(underTest.isRunning()).isFalse();
    }

    private void awaitSubscription() throws InterruptedException {
        assertThat(subscribed.await(2, TimeUnit.SECONDS)).isTrue();
    }
}
