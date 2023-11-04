package com.github.havlli.EventPilot.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import org.springframework.stereotype.Component;

@Component
public class CustomConsoleAppender extends ConsoleAppender<ILoggingEvent> {

    private final ConsoleLogPublisher logPublisher;

    public CustomConsoleAppender(ConsoleLogPublisher logPublisher) {
        this.logPublisher = logPublisher;
    }

    @Override
    protected void append(ILoggingEvent loggingEvent) {
        logPublisher.publish(loggingEvent);
    }
}
