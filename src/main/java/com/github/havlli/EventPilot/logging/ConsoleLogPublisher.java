package com.github.havlli.EventPilot.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class ConsoleLogPublisher {
    private final Sinks.Many<ILoggingEvent> sinks;

    public ConsoleLogPublisher() {
        this.sinks = initiateSink();
    }

    private Sinks.Many<ILoggingEvent> initiateSink() {
        return Sinks.many().replay().limit(50);
    }

    public void publish(ILoggingEvent eventObject) {
        sinks.emitNext(eventObject, Sinks.EmitFailureHandler.FAIL_FAST);
    }

    public Flux<ILoggingEvent> asFlux() {
        return sinks.asFlux();
    }
}
