package com.github.havlli.EventPilot.entity.event;

import com.github.havlli.EventPilot.entity.embedtype.EmbedType;
import com.github.havlli.EventPilot.entity.guild.Guild;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EventTest {

    private AutoCloseable autoCloseable;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void builder_ReturnsCompleteEvent_WhenBuildMethodCalled() {
        // Arrange
        Event.Builder builder = Event.builder();
        EmbedType embedTypeMock = mock(EmbedType.class);

        // Act
        Event actual = builder.withEventId("testId")
                .withName("testName")
                .withDescription("test")
                .withAuthor("test")
                .withDateTime(Instant.now())
                .withDestinationChannel("test")
                .withInstances(List.of("test"))
                .withMemberSize("test")
                .withGuild(new Guild("test", "test"))
                .withEmbedType(embedTypeMock)
                .build();

        // Assert
        assertThat(actual).hasNoNullFieldsOrProperties();
    }

    @Test
    void builder_ReturnsPreviewFieldsHashMap_WhenPreviewFieldsMethodCalled() {
        // Arrange
        String name = "test", description = "test", memberSize = "test", destChannel = "test";
        Instant dateTime = Instant.now();
        List<String> instances = List.of("test", "test");
        Event.Builder builder = Event.builder()
                .withEventId("testId")
                .withName(name)
                .withDescription(description)
                .withAuthor("test")
                .withDateTime(dateTime)
                .withDestinationChannel(destChannel)
                .withInstances(instances)
                .withMemberSize(memberSize)
                .withGuild(new Guild("test", "test"));

        Map<String, String> expected = Map.of(
                "name", name,
                "description", description,
                "date and time", dateTime.toString(),
                "instances", String.join(", ", instances),
                "member size", memberSize,
                "destination channel", destChannel
        );

        // Act
        HashMap<String, String> previewFields = builder.previewFields();

        // Assert
        assertThat(previewFields).containsExactlyInAnyOrderEntriesOf(expected);
    }
}