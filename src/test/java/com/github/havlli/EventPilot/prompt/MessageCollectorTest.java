package com.github.havlli.EventPilot.prompt;

import discord4j.core.object.entity.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;

class MessageCollectorTest {

    private AutoCloseable autoCloseable;
    private MessageCollector underTest;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new MessageCollector();
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void cleanup_WillReturnChainedMonoAndClearList() {
        // Arrange
        Message messageMockOne = mock(Message.class);
        Message messageMockTwo = mock(Message.class);
        when(messageMockOne.delete()).thenReturn(Mono.empty());
        when(messageMockTwo.delete()).thenReturn(Mono.empty());
        underTest.collect(messageMockOne);
        underTest.collect(messageMockTwo);

        // Act
        Mono<Void> actualMono = underTest.cleanup();

        // Assert
        verify(messageMockOne, times(1)).delete();
        verify(messageMockTwo, times(1)).delete();

        System.out.println(actualMono);
        System.out.println(Mono.just(5).zipWith(Mono.just(6)).block());
    }
}