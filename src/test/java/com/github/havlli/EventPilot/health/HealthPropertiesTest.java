package com.github.havlli.EventPilot.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class HealthPropertiesTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(HealthConfiguration.class);

    @Test
    void defaultsToDisabledAndPort8081WithoutCreatingServerOrGatewayCheck() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(HealthProperties.class)).isEqualTo(new HealthProperties(false, 8081));
            assertThat(context).doesNotHaveBean(HealthServer.class).doesNotHaveBean(GatewayHealthCheck.class);
        });
    }

    @Test
    void explicitlyDisabledDoesNotRequireDiscordOrDatabaseBeans() {
        runner.withPropertyValues("eventpilot.health.enabled=false", "eventpilot.health.port=9091")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(HealthProperties.class)).isEqualTo(new HealthProperties(false, 9091));
                    assertThat(context).doesNotHaveBean(HealthServer.class).doesNotHaveBean(GatewayHealthCheck.class);
                });
    }

    @Test
    void testProfileExcludesEnabledServerAndGatewayCheck() {
        runner.withPropertyValues("spring.profiles.active=test", "eventpilot.health.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(HealthProperties.class).enabled()).isTrue();
                    assertThat(context).doesNotHaveBean(HealthServer.class).doesNotHaveBean(GatewayHealthCheck.class);
                });
    }

    @Test
    void rejectsPortsOutsideValidRange() {
        for (int port : new int[]{0, -1, 65536}) {
            runner.withPropertyValues("eventpilot.health.port=" + port)
                    .run(context -> assertThat(context).hasFailed());
        }
    }

    @Test
    void acceptsPortRangeBoundaries() {
        for (int port : new int[]{1, 65535}) {
            runner.withPropertyValues("eventpilot.health.port=" + port)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context.getBean(HealthProperties.class).port()).isEqualTo(port);
                    });
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @ConfigurationPropertiesScan(basePackageClasses = HealthProperties.class)
    @Import({HealthServer.class, DiscordGatewayHealthCheck.class})
    static class HealthConfiguration {
    }
}
