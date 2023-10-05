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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    void builder_getDestinationChannelId_ReturnsId() {
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

        // Act
        String actual = builder.getDestinationChannelId();

        // Assert
        assertThat(actual).isEqualTo(destChannel);
    }

    @Test
    void builder_getEvent_ReturnsEvent_WhenEventAlreadyBuild() {
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
                .withGuild(new Guild("test", "test"))
                .withEmbedType(new EmbedType(1L, "test", "test", List.of()));
        builder.build();

        // Act
        Event actual = builder.getEvent();

        // Assert
        assertThat(actual).hasNoNullFieldsOrProperties();
    }

    @Test
    void builder_getEvent_ThrowsException_WhenEventNotYetBuild() {
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
                .withGuild(new Guild("test", "test"))
                .withEmbedType(new EmbedType(1L, "test", "test", List.of()));

        // Assert
        assertThatThrownBy(() -> builder.getEvent())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot retrieve event that was not built yet!");
    }

    @Test
    void Event_toString() {
        // Arrange
        String name = "test", description = "test", memberSize = "test", destChannel = "test";
        Instant dateTime = Instant.parse("2023-09-04T19:10:06.947216700Z");
        List<String> instances = List.of("test", "test");
        Event event = Event.builder()
                .withEventId("testId")
                .withName(name)
                .withDescription(description)
                .withAuthor("test")
                .withDateTime(dateTime)
                .withDestinationChannel(destChannel)
                .withInstances(instances)
                .withMemberSize(memberSize)
                .withGuild(new Guild("test", "test"))
                .withEmbedType(new EmbedType(1L, "test", "test", List.of()))
                .build();
        String expected = "Event{eventId='testId', name='test', description='test', author='test', dateTime=2023-09-04T19:10:06.947216700Z, destinationChannelId='test', instances='test, test', memberSize='test', participants=0, guild=test, embedType=test}";

        // Act
        String actual = event.toString();

        // Assert
        assertThat(actual).isEqualTo(expected);
    }
}