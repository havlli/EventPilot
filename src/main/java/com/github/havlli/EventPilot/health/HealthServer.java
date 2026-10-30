package com.github.havlli.EventPilot.health;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
@Profile("!test")
@ConditionalOnProperty(prefix = "eventpilot.health", name = "enabled", havingValue = "true")
public class HealthServer implements SmartLifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(HealthServer.class);

    private final int port;
    private final GatewayHealthCheck gateway;
    private final DataSource dataSource;
    private HttpServer server;
    private ThreadPoolExecutor executor;
    private volatile boolean running;

    @Autowired
    public HealthServer(HealthProperties properties, GatewayHealthCheck gateway, DataSource dataSource) {
        this(properties.port(), gateway, dataSource);
    }

    // Tests may request an ephemeral port without relaxing configuration validation.
    HealthServer(int port, GatewayHealthCheck gateway, DataSource dataSource) {
        this.port = port;
        this.gateway = gateway;
        this.dataSource = dataSource;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }

        HttpServer startingServer = null;
        ThreadPoolExecutor startingExecutor = null;
        try {
            startingServer = HttpServer.create(
                    new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port), 16);
            startingExecutor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(8), task -> {
                        Thread thread = new Thread(task, "eventpilot-health");
                        thread.setDaemon(true);
                        return thread;
                    }, new ThreadPoolExecutor.AbortPolicy());
            startingServer.setExecutor(startingExecutor);
            startingServer.createContext("/", this::handle);
            startingServer.start();
            server = startingServer;
            executor = startingExecutor;
            running = true;
            LOG.info("Health server listening on {}", server.getAddress());
        } catch (IOException | RuntimeException e) {
            if (startingServer != null) {
                startingServer.stop(0);
            }
            if (startingExecutor != null) {
                startingExecutor.shutdownNow();
            }
            throw new IllegalStateException("Could not start loopback health server", e);
        }
    }

    @Override
    public synchronized void stop() {
        if (!running) {
            return;
        }
        try {
            server.stop(0);
        } finally {
            executor.shutdownNow();
            server = null;
            executor = null;
            running = false;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    synchronized InetSocketAddress getAddress() {
        if (!running) {
            throw new IllegalStateException("Health server is not running");
        }
        return server.getAddress();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!"/health".equals(exchange.getRequestURI().getPath())) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "GET");
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            boolean discordUp = discordUp();
            boolean databaseUp = databaseUp();
            boolean healthy = discordUp && databaseUp;
            byte[] body = ("{\"status\":\"" + status(healthy)
                    + "\",\"discord\":\"" + status(discordUp)
                    + "\",\"database\":\"" + status(databaseUp) + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(healthy ? 200 : 503, body.length);
            exchange.getResponseBody().write(body);
        }
    }

    private boolean discordUp() {
        try {
            return gateway.isConnected();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean databaseUp() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (SQLException | RuntimeException e) {
            return false;
        }
    }

    private static String status(boolean up) {
        return up ? "UP" : "DOWN";
    }
}
