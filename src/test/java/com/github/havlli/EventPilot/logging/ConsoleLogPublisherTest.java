package com.github.havlli.EventPilot.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ConsoleLogPublisherTest {

    private ConsoleLogPublisher underTest;
    private AutoCloseable autoCloseable;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new ConsoleLogPublisher();
    }

    @AfterEach
    void teardown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void publish_SinkAcceptsLoggingEvent() {
        // Arrange
        ILoggingEvent loggingEventMock = mock(ILoggingEvent.class);

        // Act & Assert
        StepVerifier.create(underTest.asFlux())
                .then(() -> underTest.publish(loggingEventMock))
                .expectNext(loggingEventMock)
                .thenCancel()
                .verify();
    }

    @Test
    void asFlux() {
        // Act
        Flux<ILoggingEvent> actual = underTest.asFlux();

        // Assert
        assertThat(actual).isInstanceOf(Flux.class);
    }
}