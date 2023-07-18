package com.github.havlli.EventPilot.core;

import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import discord4j.rest.service.ApplicationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.ApplicationArguments;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GlobalCommandRegistrarTest {

    private AutoCloseable autoCloseable;
    private GlobalCommandRegistrar underTest;
    @Mock
    private ApplicationService applicationServiceMock;
    @Mock
    private ApplicationArguments applicationArguments;
    @Captor
    private ArgumentCaptor<List<ApplicationCommandRequest>> commandsCaptor;
    @Mock
    private RestClient mockClient;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new GlobalCommandRegistrar(mockClient);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void run_ShouldBulkOverwriteGlobalApplicationCommands() throws IOException {
        // Arrange
        when(mockClient.getApplicationService()).thenReturn(applicationServiceMock);
        when(mockClient.getApplicationId()).thenReturn(Mono.just(123L));

        when(applicationServiceMock.bulkOverwriteGlobalApplicationCommand(anyLong(), commandsCaptor.capture()))
                .thenReturn(Flux.empty());

        // Act
        underTest.run(applicationArguments);

        // Assert
        verify(applicationServiceMock, times(1))
                .bulkOverwriteGlobalApplicationCommand(eq(123L), anyList());

        List<ApplicationCommandRequest> capturedCommands = commandsCaptor.getValue();
        assertThat(capturedCommands.size()).isEqualTo(5);
    }
}