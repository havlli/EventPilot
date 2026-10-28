package com.github.havlli.EventPilot.core;

import discord4j.core.GatewayDiscordClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Profile("!test")
public class GatewayKeepAlive implements SmartLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(GatewayKeepAlive.class);
    private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(5);

    private final GatewayDiscordClient gateway;
    private volatile boolean running;
    private volatile boolean stopping;
    private Thread keepAliveThread;

    public GatewayKeepAlive(GatewayDiscordClient gateway) {
        this.gateway = gateway;
    }

    @Override
    public synchronized void start() {
        if (keepAliveThread != null || stopping) {
            return;
        }

        running = true;
        keepAliveThread = new Thread(() -> {
            try {
                gateway.onDisconnect().block();
            } catch (RuntimeException e) {
                if (!stopping) {
                    LOG.warn("Discord gateway disconnected with an error", e);
                }
            } finally {
                running = false;
            }
        }, "discord-gateway-keep-alive");
        keepAliveThread.setDaemon(false);
        keepAliveThread.start();
    }

    @Override
    public synchronized void stop() {
        if (stopping || keepAliveThread == null) {
            return;
        }
        stopping = true;

        try {
            gateway.logout().block(SHUTDOWN_TIMEOUT);
        } catch (RuntimeException e) {
            LOG.warn("Discord gateway logout did not complete", e);
        } finally {
            // Release the waiter even if the gateway fails to signal disconnection on logout.
            keepAliveThread.interrupt();
            try {
                keepAliveThread.join(SHUTDOWN_TIMEOUT.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
