package com.github.havlli.EventPilot.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.time.Instant;

public record ConsoleLogEventDTO(
        String message,
        String threadName,
        String loggerName,
        Level loggerLevel,
        Instant timestamp,
        ThrowableProxyDTO throwableProxy
) {
    public static ConsoleLogEventDTO fromLoggingEvent(ILoggingEvent event) {
        return new ConsoleLogEventDTO(
                event.getMessage(),
                event.getThreadName(),
                event.getLoggerName(),
                event.getLevel(),
                event.getInstant(),
                event.getThrowableProxy() != null ?
                        ThrowableProxyDTO.fromThrowableProxy(event.getThrowableProxy()) : null
        );
    }
}
