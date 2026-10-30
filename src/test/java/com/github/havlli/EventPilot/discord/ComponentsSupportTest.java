package com.github.havlli.EventPilot.discord;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.havlli.EventPilot.component.ButtonRow;
import com.github.havlli.EventPilot.component.selectmenu.CustomSelectMenu;
import discord4j.common.JacksonResources;
import discord4j.core.object.RecurrenceRule;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.CheckboxAction;
import discord4j.core.object.component.CheckboxGroupAction;
import discord4j.core.object.component.Container;
import discord4j.core.object.component.Label;
import discord4j.core.object.component.MessageComponent;
import discord4j.core.object.component.RadioGroupAction;
import discord4j.core.object.component.Section;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.component.Separator;
import discord4j.core.object.component.StringSelectMenu;
import discord4j.core.object.component.TextDisplay;
import discord4j.core.object.component.TextInput;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.ScheduledEvent;
import discord4j.core.spec.InteractionPresentModalSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.RecurrenceRuleSpec;
import discord4j.core.spec.ScheduledEventCreateSpec;
import discord4j.core.spec.ScheduledEventEntityMetadataSpec;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Offline JSON-shape checks using Discord4J 3.3.3 specs and its Jackson mapper (discord-json 1.7.24).
 * Live Discord behaviour is unverified by these tests, including validation and rendering.
 * All requested components are constructible; none are skipped or replaced with hand-built JSON.
 * MessageComponent.Type in the 3.3.3 jar confirms radio group 21, checkbox group 22 and checkbox 23.
 */
class ComponentsSupportTest {

    private final ObjectMapper mapper = JacksonResources.create().getObjectMapper();

    @Test
    void componentsV2MessageSerializesNestedComponentsAndAutomaticallySetsFlag() throws IOException {
        MessageCreateSpec spec = MessageCreateSpec.builder()
                .components(Container.of(
                        Section.of(Button.primary("signup", "Sign up"), TextDisplay.of("Raid signup")),
                        TextDisplay.of("Choose a role"),
                        Separator.of()))
                .build();

        JsonNode json = serialize(spec.asRequest().getJsonPayload());

        assertThat(Message.Flag.IS_COMPONENTS_V2.getFlag()).isEqualTo(1 << 15);
        assertThat(json.path("flags").isIntegralNumber()).isTrue();
        assertThat(json.path("flags").intValue() & (1 << 15)).isEqualTo(1 << 15);
        assertThat(json.path("components").size()).isEqualTo(1);
        JsonNode container = json.at("/components/0");
        assertType(container, MessageComponent.Type.CONTAINER, 17);
        assertThat(container.path("components").size()).isEqualTo(3);
        JsonNode section = container.at("/components/0");
        assertType(section, MessageComponent.Type.SECTION, 9);
        assertType(section.at("/components/0"), MessageComponent.Type.TEXT_DISPLAY, 10);
        assertThat(section.at("/components/0/content").asText()).isEqualTo("Raid signup");
        assertType(section.path("accessory"), MessageComponent.Type.BUTTON, 2);
        assertThat(section.at("/accessory/custom_id").asText()).isEqualTo("signup");
        assertType(container.at("/components/1"), MessageComponent.Type.TEXT_DISPLAY, 10);
        assertThat(container.at("/components/1/content").asText()).isEqualTo("Choose a role");
        assertType(container.at("/components/2"), MessageComponent.Type.SEPARATOR, 14);
    }

    @Test
    void modalSerializesLabelsWithTextInputStringSelectRadioAndCheckboxGroups() throws IOException {
        InteractionPresentModalSpec spec = InteractionPresentModalSpec.builder()
                .title("Event preferences").customId("preferences")
                .components(
                        Label.of("Name", TextInput.small("name")),
                        Label.of("Role", StringSelectMenu.of("role", SelectMenu.Option.of("Tank", "tank"))),
                        Label.of("Time", RadioGroupAction.of("time",
                                List.of(RadioGroupAction.Option.of("Evening", "evening")))),
                        Label.of("Days", CheckboxGroupAction.of("days",
                                List.of(CheckboxGroupAction.Option.of("Friday", "friday")))),
                        TextDisplay.of("Choose your preferences"))
                .build();

        JsonNode json = serialize(spec.asRequest());

        assertThat(json.path("title").asText()).isEqualTo("Event preferences");
        assertThat(json.path("custom_id").asText()).isEqualTo("preferences");
        assertThat(json.path("components").size()).isEqualTo(5);
        for (int i = 0; i < 4; i++) {
            assertType(json.path("components").get(i), MessageComponent.Type.LABEL, 18);
        }
        assertType(json.at("/components/0/component"), MessageComponent.Type.TEXT_INPUT, 4);
        assertThat(json.at("/components/0/component/custom_id").asText()).isEqualTo("name");
        assertType(json.at("/components/1/component"), MessageComponent.Type.SELECT_MENU_STRING, 3);
        assertThat(json.at("/components/1/component/options/0/value").asText()).isEqualTo("tank");
        assertType(json.at("/components/2/component"), MessageComponent.Type.RADIO_GROUP, 21);
        assertThat(json.at("/components/2/component/options/0/value").asText()).isEqualTo("evening");
        assertType(json.at("/components/3/component"), MessageComponent.Type.CHECKBOX_GROUP, 22);
        assertThat(json.at("/components/3/component/options/0/value").asText()).isEqualTo("friday");
        assertType(json.at("/components/4"), MessageComponent.Type.TEXT_DISPLAY, 10);
        assertThat(json.at("/components/4/content").asText()).isEqualTo("Choose your preferences");
    }

    @Test
    void modalSerializesStandaloneCheckboxInLabel() throws IOException {
        // Separate modal to keep the previous example within five top-level components.
        InteractionPresentModalSpec spec = InteractionPresentModalSpec.builder()
                .title("Confirmation").customId("confirmation")
                .components(Label.of("Notify me", CheckboxAction.of("notify")))
                .build();

        JsonNode json = serialize(spec.asRequest());

        assertType(json.at("/components/0"), MessageComponent.Type.LABEL, 18);
        assertType(json.at("/components/0/component"), MessageComponent.Type.CHECKBOX, 23);
        assertThat(json.at("/components/0/component/custom_id").asText()).isEqualTo("notify");
    }

    @Test
    void externalScheduledEventSerializesLocationAndRecurrenceRule() throws IOException {
        Instant start = Instant.parse("2026-11-06T18:00:00Z");
        ScheduledEventCreateSpec spec = ScheduledEventCreateSpec.builder()
                .name("Weekly raid")
                .privacyLevel(ScheduledEvent.PrivacyLevel.GUILD_ONLY)
                .scheduledStartTime(start)
                .scheduledEndTime(start.plusSeconds(3600))
                .entityType(ScheduledEvent.EntityType.EXTERNAL)
                .entityMetadata(ScheduledEventEntityMetadataSpec.builder().location("Raid lobby").build())
                .recurrenceRule(RecurrenceRuleSpec.builder()
                        .start(start).frequency(RecurrenceRule.Frequency.WEEKLY).interval(1)
                        .byWeekday(List.of(DayOfWeek.FRIDAY)).count(4).build())
                .build();

        JsonNode json = serialize(spec.asRequest());

        assertThat(json.path("name").asText()).isEqualTo("Weekly raid");
        assertThat(json.path("entity_type").intValue()).isEqualTo(3);
        assertThat(Instant.parse(json.path("scheduled_start_time").asText())).isEqualTo(start);
        assertThat(Instant.parse(json.path("scheduled_end_time").asText())).isEqualTo(start.plusSeconds(3600));
        assertThat(json.at("/entity_metadata/location").asText()).isEqualTo("Raid lobby");
        JsonNode rule = json.path("recurrence_rule");
        assertThat(Instant.parse(rule.path("start").asText())).isEqualTo(start);
        assertThat(rule.path("frequency").intValue()).isEqualTo(2);
        assertThat(rule.path("interval").intValue()).isEqualTo(1);
        assertThat(rule.path("by_weekday").size()).isEqualTo(1);
        assertThat(rule.at("/by_weekday/0").intValue()).isEqualTo(4);
        assertThat(rule.path("count").intValue()).isEqualTo(4);
    }

    @Test
    void legacyCustomSelectMenuAndButtonRowKeepTheirJsonTypes() throws IOException {
        CustomSelectMenu select = new CustomSelectMenu("destination", "Choose channel", Map.of(123L, "events"));
        ButtonRow buttons = ButtonRow.builder()
                .addButton("signup", "Sign up", ButtonRow.Builder.ButtonType.PRIMARY).build();
        MessageCreateSpec spec = MessageCreateSpec.builder()
                .components(select.getActionRow(), buttons.getActionRow()).build();

        JsonNode json = serialize(spec.asRequest().getJsonPayload());

        assertType(json.at("/components/0"), MessageComponent.Type.ACTION_ROW, 1);
        assertType(json.at("/components/0/components/0"), MessageComponent.Type.SELECT_MENU_STRING, 3);
        assertThat(json.at("/components/0/components/0/options/0/label").asText()).isEqualTo("events");
        assertThat(json.at("/components/0/components/0/options/0/value").asText()).isEqualTo("123");
        assertType(json.at("/components/1"), MessageComponent.Type.ACTION_ROW, 1);
        assertType(json.at("/components/1/components/0"), MessageComponent.Type.BUTTON, 2);
        assertThat(json.at("/components/1/components/0/custom_id").asText()).isEqualTo("signup");
        assertThat(json.path("flags").asInt(0) & (1 << 15)).isZero();
    }

    private JsonNode serialize(Object request) throws IOException {
        return mapper.readTree(mapper.writeValueAsBytes(request));
    }

    private void assertType(JsonNode component, MessageComponent.Type type, int expected) {
        assertThat(type.getValue()).isEqualTo(expected);
        assertThat(component.path("type").isIntegralNumber()).isTrue();
        assertThat(component.path("type").intValue()).isEqualTo(expected);
    }
}
