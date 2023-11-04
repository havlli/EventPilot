package com.github.havlli.EventPilot.logging;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ThrowableProxyDTOTest {

    @Test
    void fromThrowableProxy() {
        // Arrange
        IThrowableProxy throwableProxy = mock(IThrowableProxy.class);
        String className = "com.github.havlli.EventPilot.logging.ThrowableProxyDTO";
        String message = "message";
        String stackTrace = "stackTrace";
        when(throwableProxy.getClassName()).thenReturn(className);
        when(throwableProxy.getMessage()).thenReturn(message);
        StackTraceElementProxy traceElementProxyMock = mock(StackTraceElementProxy.class);
        when(traceElementProxyMock.getSTEAsString()).thenReturn(stackTrace);
        when(throwableProxy.getStackTraceElementProxyArray()).thenReturn(new StackTraceElementProxy[]{traceElementProxyMock});

        // Act
        ThrowableProxyDTO actual = ThrowableProxyDTO.fromThrowableProxy(throwableProxy);

        // Assert
        assertThat(actual.className()).isEqualTo(className);
        assertThat(actual.message()).isEqualTo(message);
        assertThat(actual.stackTrace()).contains(stackTrace);

    }
}