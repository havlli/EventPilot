package com.github.havlli.EventPilot.entity.event;

import com.github.havlli.EventPilot.core.DiscordProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class EventSettingsResolverTest {

    @Test
    void reminderLead_EventOverrideWins() {
        EventSettingsResolver underTest = resolver(90);
        Event event = Event.builder().withReminderLeadMinutes(15).build();

        assertThat(underTest.reminderLead(event)).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void reminderLead_NullOverrideUsesInstanceSetting() {
        EventSettingsResolver underTest = resolver(90);

        assertThat(underTest.reminderLead(Event.builder().build())).isEqualTo(Duration.ofMinutes(90));
        assertThat(underTest.instanceReminderLead()).isEqualTo(Duration.ofMinutes(90));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void reminderLead_InvalidInstanceSettingUsesBuiltInDefault(int minutes) {
        assertThat(resolver(minutes).reminderLead(Event.builder().build())).isEqualTo(Duration.ofMinutes(60));
    }

    @Test
    void reminderLead_MissingInstanceSettingUsesBuiltInDefault() {
        EventSettingsResolver underTest = new EventSettingsResolver(new DiscordProperties("test", null, null));

        assertThat(underTest.reminderLead(Event.builder().build())).isEqualTo(Duration.ofMinutes(60));
        assertThat(new EventSettingsResolver(null).instanceReminderLead()).isEqualTo(Duration.ofMinutes(60));
    }

    @Test
    void reminderLead_OverrideWinsEvenWithMissingInstanceSetting() {
        Event event = Event.builder().withReminderLeadMinutes(1).build();

        assertThat(new EventSettingsResolver(null).reminderLead(event)).isEqualTo(Duration.ofMinutes(1));
    }

    private EventSettingsResolver resolver(int minutes) {
        return new EventSettingsResolver(new DiscordProperties(
                "test", null, new DiscordProperties.Scheduler(60, minutes)));
    }
}
