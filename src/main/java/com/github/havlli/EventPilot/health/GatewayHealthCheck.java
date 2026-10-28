package com.github.havlli.EventPilot.health;

@FunctionalInterface
public interface GatewayHealthCheck {
    boolean isConnected();
}
