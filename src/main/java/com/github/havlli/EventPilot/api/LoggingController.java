package com.github.havlli.EventPilot.api;

import com.github.havlli.EventPilot.logging.ConsoleLogEventDTO;
import com.github.havlli.EventPilot.logging.ConsoleLogPublisher;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/logging")
public class LoggingController {

    private final ConsoleLogPublisher consoleLogPublisher;

    public LoggingController(ConsoleLogPublisher consoleLogPublisher) {
        this.consoleLogPublisher = consoleLogPublisher;
    }

    @GetMapping("stream-sse")
    public Flux<ServerSentEvent<ConsoleLogEventDTO>> sse() {
        return consoleLogPublisher.asFlux()
                .map(loggingEvent -> ServerSentEvent.<ConsoleLogEventDTO> builder()
                        .id(String.valueOf(loggingEvent.getInstant().toEpochMilli()))
                        .data(ConsoleLogEventDTO.fromLoggingEvent(loggingEvent))
                        .build());
    }
}
