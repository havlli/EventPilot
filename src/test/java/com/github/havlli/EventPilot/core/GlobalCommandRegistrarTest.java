package com.github.havlli.EventPilot.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import discord4j.common.JacksonResources;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
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
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        underTest = new GlobalCommandRegistrar(
                mockClient,
                new PathMatchingResourcePatternResolver(),
                discordProperties("commands")
        );
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
        Map<String, ApplicationCommandRequest> commandsByName = capturedCommands.stream()
                .collect(Collectors.toMap(ApplicationCommandRequest::name, command -> command));

        assertThat(capturedCommands)
                .extracting(ApplicationCommandRequest::name)
                .containsExactlyInAnyOrder(
                        "cancel-event",
                        "clear-expired",
                        "close-event",
                        "create-embed-type",
                        "create-event",
                        "delete-event",
                        "event-info",
                        "list-events",
                        "reopen-event"
                );
        assertThat(commandsByName).hasSize(9);
        assertThat(capturedCommands)
                .allSatisfy(command -> assertThat(command.defaultMemberPermissions()).contains("16"));
        assertMessageIdOption(commandsByName.get("cancel-event"));
        assertMessageIdOption(commandsByName.get("close-event"));
        assertMessageIdOption(commandsByName.get("delete-event"));
        assertMessageIdOption(commandsByName.get("event-info"));
        assertMessageIdOption(commandsByName.get("reopen-event"));
        assertListEventsOptions(commandsByName.get("list-events"));
    }

    @Test
    void run_ShouldThrow_WhenFileNotFound() {
        // Arrange
        when(mockClient.getApplicationService()).thenReturn(applicationServiceMock);
        when(mockClient.getApplicationId()).thenReturn(Mono.just(123L));

        underTest = new GlobalCommandRegistrar(
                mockClient,
                new PathMatchingResourcePatternResolver(),
                discordProperties("impossible-folder")
        );

        // Assert
        assertThatThrownBy(() -> underTest.run(applicationArguments))
                .isInstanceOfAny(IOException.class);

        verifyNoInteractions(applicationServiceMock);
    }

    @Test
    void run_ShouldThrowRuntimeException_WhenReadFailed() throws IOException {
        // Arrange
        JacksonResources jacksonResourcesMock = mock(JacksonResources.class);
        Resource resourceMock = mock(Resource.class);
        when(resourceMock.getInputStream()).thenReturn(null);

        GlobalCommandRegistrar underTestSpy = spy(underTest);
        ObjectMapper objectMapperMock = mock(ObjectMapper.class);
        when(jacksonResourcesMock.getObjectMapper()).thenReturn(objectMapperMock);
        when(underTestSpy.readJsonValue(jacksonResourcesMock, resourceMock)).thenThrow(new IOException());

        // Assert
        assertThatThrownBy(() -> underTestSpy.readJsonValueOrThrow(jacksonResourcesMock, resourceMock))
                .isInstanceOfAny(RuntimeException.class);

        verifyNoInteractions(applicationServiceMock);
    }

    @Test
    void run_ShouldThrow_WhenBulkOverwriteFails() {
        // Arrange
        when(mockClient.getApplicationService()).thenReturn(applicationServiceMock);
        when(mockClient.getApplicationId()).thenReturn(Mono.just(123L));
        when(applicationServiceMock.bulkOverwriteGlobalApplicationCommand(anyLong(), anyList()))
                .thenReturn(Flux.error(new RuntimeException("registration failed")));

        // Assert
        assertThatThrownBy(() -> underTest.run(applicationArguments))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("registration failed");
    }

    private DiscordProperties discordProperties(String commandsFolder) {
        return new DiscordProperties(
                "token",
                new DiscordProperties.Commands(commandsFolder),
                new DiscordProperties.Scheduler(60, 60)
        );
    }

    private void assertMessageIdOption(ApplicationCommandRequest command) {
        ApplicationCommandOptionData option = findOption(command, "message-id");

        assertThat(option.type()).isEqualTo(3);
        assertThat(option.required().toOptional()).contains(true);
        assertThat(option.description()).isEqualTo("Discord message ID of the event signup");
    }

    private void assertListEventsOptions(ApplicationCommandRequest command) {
        ApplicationCommandOptionData statusOption = findOption(command, "status");
        ApplicationCommandOptionData limitOption = findOption(command, "limit");

        assertThat(statusOption.type()).isEqualTo(3);
        assertThat(statusOption.required().toOptional()).contains(false);
        assertThat(statusOption.choices().toOptional().orElse(List.of()))
                .extracting(ApplicationCommandOptionChoiceData::value)
                .containsExactly("active", "open", "closed", "cancelled", "expired", "all");

        assertThat(limitOption.type()).isEqualTo(4);
        assertThat(limitOption.required().toOptional()).contains(false);
        assertThat(limitOption.minValue().toOptional()).contains(1.0);
        assertThat(limitOption.maxValue().toOptional()).contains(10.0);
    }

    private ApplicationCommandOptionData findOption(ApplicationCommandRequest command, String optionName) {
        assertThat(command).isNotNull();
        List<ApplicationCommandOptionData> options = command.options().toOptional().orElse(List.of());
        Set<String> optionNames = options.stream()
                .map(ApplicationCommandOptionData::name)
                .collect(Collectors.toSet());

        assertThat(optionNames).contains(optionName);
        return options.stream()
                .filter(option -> option.name().equals(optionName))
                .findFirst()
                .orElseThrow();
    }
}
