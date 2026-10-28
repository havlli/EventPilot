package com.github.havlli.EventPilot.health;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class HealthServerTest {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2)).build();

    private GatewayHealthCheck gateway;
    private DataSource dataSource;
    private Connection connection;
    private HealthServer server;

    @BeforeEach
    void setUp() throws SQLException {
        gateway = mock(GatewayHealthCheck.class);
        dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        when(gateway.isConnected()).thenReturn(true);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);
        server = new HealthServer(0, gateway, dataSource);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void returnsUpWhenBothChecksPassOnDaemonWorker() throws Exception {
        when(gateway.isConnected()).thenAnswer(invocation -> {
            assertThat(Thread.currentThread().isDaemon()).isTrue();
            return true;
        });

        HttpResponse<String> response = request("GET", "/health");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"status\":\"UP\",\"discord\":\"UP\",\"database\":\"UP\"}");
        assertThat(response.headers().firstValue("Content-Type"))
                .contains("application/json; charset=utf-8");
        verify(connection).isValid(2);
        verify(connection).close();
    }

    @Test
    void returnsDownWhenGatewayDisconnected() throws Exception {
        when(gateway.isConnected()).thenReturn(false);

        assertDown("DOWN", "UP");
        verify(connection).close();
    }

    @Test
    void returnsDownWhenGatewayCheckThrows() throws Exception {
        when(gateway.isConnected()).thenThrow(new IllegalStateException("private gateway detail"));

        assertDown("DOWN", "UP");
    }

    @Test
    void returnsDownWhenDatabaseInvalid() throws Exception {
        when(connection.isValid(2)).thenReturn(false);

        assertDown("UP", "DOWN");
        verify(connection).close();
    }

    @Test
    void returnsDownWhenDatabaseConnectionThrows() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("private database detail"));

        assertDown("UP", "DOWN");
    }

    @Test
    void returnsDownAndClosesConnectionWhenValidationThrows() throws Exception {
        when(connection.isValid(2)).thenThrow(new SQLException("private validation detail"));

        assertDown("UP", "DOWN");
        verify(connection).close();
    }

    @Test
    void namesBothFailedChecks() throws Exception {
        when(gateway.isConnected()).thenReturn(false);
        when(connection.isValid(2)).thenReturn(false);

        assertDown("DOWN", "DOWN");
    }

    @Test
    void returnsNotFoundForOtherPathsIncludingHealthSuffix() throws Exception {
        for (String path : new String[]{"/", "/other", "/health/", "/health-extra"}) {
            assertThat(request("GET", path).statusCode()).isEqualTo(404);
        }
        verifyNoInteractions(gateway, dataSource);
    }

    @Test
    void rejectsPostWithoutRunningChecks() throws Exception {
        HttpResponse<String> response = request("POST", "/health");

        assertThat(response.statusCode()).isEqualTo(405);
        assertThat(response.headers().firstValue("Allow")).contains("GET");
        verifyNoInteractions(gateway, dataSource);
    }

    @Test
    void bindsOnlyToIpv4Loopback() {
        assertThat(server.getAddress().getAddress().isLoopbackAddress()).isTrue();
        assertThat(server.getAddress().getAddress().getHostAddress()).isEqualTo("127.0.0.1");
    }

    @Test
    void startAndStopAreIdempotentAndReleasePort() throws Exception {
        InetSocketAddress address = server.getAddress();
        server.start();
        assertThat(server.getAddress()).isEqualTo(address);
        assertThat(server.isRunning()).isTrue();

        server.stop();
        server.stop();

        assertThat(server.isRunning()).isFalse();
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(true);
            socket.bind(address);
            assertThat(socket.getLocalSocketAddress()).isEqualTo(address);
        }
        server.start();
        assertThat(request("GET", "/health").statusCode()).isEqualTo(200);
    }

    @Test
    void stopBeforeStartAndStopCallbackAreSafe() {
        server.stop();
        server = new HealthServer(0, gateway, dataSource);
        server.stop();
        server.start();

        boolean[] callbackInvoked = {false};
        server.stop(() -> callbackInvoked[0] = true);

        assertThat(callbackInvoked[0]).isTrue();
        assertThat(server.isRunning()).isFalse();
    }

    @Test
    void bindFailureLeavesServerStopped() throws Exception {
        InetSocketAddress occupied = server.getAddress();
        HealthServer other = new HealthServer(occupied.getPort(), gateway, dataSource);
        try {
            assertThatThrownBy(other::start).isInstanceOf(IllegalStateException.class);
            assertThat(other.isRunning()).isFalse();
        } finally {
            other.stop();
        }
        assertThat(request("GET", "/health").statusCode()).isEqualTo(200);
    }

    private void assertDown(String discord, String database) throws Exception {
        HttpResponse<String> response = request("GET", "/health");
        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.body()).isEqualTo("{\"status\":\"DOWN\",\"discord\":\"" + discord
                + "\",\"database\":\"" + database + "\"}");
    }

    private HttpResponse<String> request(String method, String path) throws Exception {
        URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5))
                .method(method, HttpRequest.BodyPublishers.noBody()).build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
