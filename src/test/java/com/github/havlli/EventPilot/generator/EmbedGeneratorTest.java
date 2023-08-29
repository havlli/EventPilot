package com.github.havlli.EventPilot.generator;

import com.github.havlli.EventPilot.entity.embedtype.EmbedTypeService;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventService;
import com.github.havlli.EventPilot.entity.participant.ParticipantService;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.spec.EmbedCreateSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class EmbedGeneratorTest {

    private AutoCloseable autoCloseable;
    private EmbedGenerator underTest;
    @Mock
    private EmbedPreviewable embedPreviewableMock;
    @Mock
    private GatewayDiscordClient clientMock;
    @Mock
    private EmbedFormatter embedFormatterMock;
    @Mock
    private ComponentGenerator componentGeneratorMock;
    @Mock
    private ParticipantService participantServiceMock;
    @Mock
    private EventService eventServiceMock;
    @Mock
    private EmbedTypeService embedTypeServiceMock;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new EmbedGenerator(
                clientMock,
                embedFormatterMock,
                componentGeneratorMock,
                participantServiceMock,
                eventServiceMock,
                embedTypeServiceMock
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void generatePreview_ReturnsCorrectList_WhenMultipleFieldsAlreadySet() {
        // Arrange
        Event.Builder builder = Event.builder()
                .withName("test")
                .withDescription("test")
                .withDestinationChannel("testId")
                .withDateTime(Instant.now());
        when(embedPreviewableMock.previewFields()).thenReturn(builder.previewFields());

        // Act
        EmbedCreateSpec embedCreateSpec = underTest.generatePreview(embedPreviewableMock);

        // Assert
        assertThat(embedCreateSpec.fields()).hasSize(4);
    }

    @Test
    void generatePreview_ReturnsValidEmbedCreateSpec_WhenNoFieldsAreSet() {
        // Arrange
        Event.Builder builder = Event.builder();
        when(embedPreviewableMock.previewFields()).thenReturn(builder.previewFields());

        // Act
        EmbedCreateSpec embedCreateSpec = underTest.generatePreview(embedPreviewableMock);

        // Assert
        assertThat(embedCreateSpec.fields()).hasSize(0);
    }
}