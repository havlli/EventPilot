package com.github.havlli.EventPilot.prompt;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MessageCollectorTest {

    private AutoCloseable autoCloseable;
    private MessageCollector underTest;

    @BeforeEach
    public void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new MessageCollector();
    }

    @AfterEach
    public void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    public void collect_WillAddMessage() {
        // Arrange
        Message message = mock(Message.class);

        // Act
        underTest.collect(message);

        // Assert
        List<Message> actualMessageList = underTest.getMessageList();
        assertThat(actualMessageList).hasSize(1);
        assertThat(actualMessageList).containsOnly(message);
    }

    @Test
    public void cleanup_WillReturnEmptyFlux_WhenListIsEmpty() {
        StepVerifier.create(underTest.cleanup())
                .expectComplete()
                .verify();
    }

    @Test
    void cleanup_WillReturnChainedMonoAndClearList() {
        // Arrange
        Message messageOneMock = mock(Message.class);
        Message messageTwoMock = mock(Message.class);

        when(messageOneMock.getId()).thenReturn(Snowflake.of(1L));
        when(messageTwoMock.getId()).thenReturn(Snowflake.of(2L));

        underTest.collect(messageOneMock);
        underTest.collect(messageTwoMock);

        when(messageOneMock.delete()).thenReturn(Mono.empty());
        when(messageTwoMock.delete()).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(underTest.cleanup())
                .expectComplete()
                .verify();

        // Assert
        verify(messageOneMock, times(1)).delete();
        verify(messageTwoMock, times(1)).delete();
    }
}