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
        assertThat(actual).hasNoNullFieldsOrPropertiesExcept("reminderLeadMinutes");
        assertThat(actual.getStatus()).isEqualTo(EventStatus.OPEN);
        assertThat(actual.isReminderSent()).isFalse();
        assertThat(actual.getReminderLeadMinutes()).isNull();
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
        assertThat(actual).hasNoNullFieldsOrPropertiesExcept("reminderLeadMinutes");
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
    void builderFrom_PreservesReminderOverrideAndSentState() {
        Event original = Event.builder().withReminderLeadMinutes(120).withReminderSent(true).build();

        Event copy = Event.builder().fromEvent(original).build();

        assertThat(copy.getReminderLeadMinutes()).isEqualTo(120);
        assertThat(copy.isReminderSent()).isTrue();
        original.setReminderLeadMinutes(null);
        assertThat(Event.builder().fromEvent(original).build().getReminderLeadMinutes()).isNull();
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
        String expected = "Event{eventId='testId', name='test', description='test', author='test', dateTime=2023-09-04T19:10:06.947216700Z, destinationChannelId='test', instances='test, test', memberSize='test', status=OPEN, reminderSent=false, reminderLeadMinutes=null, participants=0, guild=test, embedType=test}";

        // Act
        String actual = event.toString();

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void equalsAndHashCode_ConsiderReminderLeadMinutes() {
        Instant dateTime = Instant.parse("2026-11-01T18:00:00Z");
        Guild guild = new Guild("1", "guild");
        Event first = Event.builder().withEventId("1").withName("name").withDateTime(dateTime)
                .withGuild(guild).build();
        Event second = Event.builder().withEventId("1").withName("name").withDateTime(dateTime)
                .withGuild(guild).build();

        assertThat(first).isEqualTo(first).isEqualTo(second).hasSameHashCodeAs(second)
                .isNotEqualTo(null).isNotEqualTo("1");

        second.setReminderLeadMinutes(30);
        assertThat(first).isNotEqualTo(second);
        first.setReminderLeadMinutes(30);
        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
        second.setReminderSent(true);
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void equals_DetectsDifferenceInEveryField() throws Exception {
        for (java.lang.reflect.Field field : Event.class.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            Event first = fullEvent();
            Event second = fullEvent();
            assertThat(first).isEqualTo(second);

            field.set(second, differentValue(field.getType()));

            assertThat(first).as(field.getName()).isNotEqualTo(second);
        }
    }

    private static Event fullEvent() {
        Event event = Event.builder().withEventId("1").withName("name").withDescription("description")
                .withAuthor("author").withDateTime(Instant.parse("2026-11-01T18:00:00Z"))
                .withDestinationChannel("channel").withInstances(List.of("instance")).withMemberSize("5")
                .withGuild(new Guild("1", "guild")).withEmbedType(EMBED_TYPE).build();
        event.setReminderLeadMinutes(30);
        return event;
    }

    private static final EmbedType EMBED_TYPE = mock(EmbedType.class);

    private static Object differentValue(Class<?> type) {
        if (type == String.class) return "other";
        if (type == Instant.class) return Instant.parse("2026-12-01T18:00:00Z");
        if (type == EventStatus.class) return EventStatus.EXPIRED;
        if (type == boolean.class) return true;
        if (type == Integer.class) return 60;
        if (type == List.class) return List.of(mock(com.github.havlli.EventPilot.entity.participant.Participant.class));
        if (type == Guild.class) return new Guild("2", "other");
        if (type == EmbedType.class) return mock(EmbedType.class);
        throw new IllegalArgumentException("Unhandled field type " + type);
    }
}
