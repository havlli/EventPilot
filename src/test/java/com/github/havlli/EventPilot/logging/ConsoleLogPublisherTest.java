package com.github.havlli.EventPilot.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.stream.IntStream;

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
    void publish_SinkAcceptsLoggingEventAndReplaysLast50EventsForFutureSubscribers() {
        // Arrange
        ILoggingEvent loggingEventMock = mock(ILoggingEvent.class);

        // Act & Assert
        StepVerifier.create(underTest.asFlux())
                .then(() -> IntStream.iterate(0, i -> i + 1)
                        .limit(60)
                        .forEach(__ -> underTest.publish(loggingEventMock)))
                .expectNextCount(50)
                .thenCancel()
                .verify();

        StepVerifier.create(underTest.asFlux())
                .expectNextCount(50)
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