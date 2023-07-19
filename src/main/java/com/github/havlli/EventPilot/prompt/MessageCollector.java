package com.github.havlli.EventPilot.prompt;

import discord4j.core.object.entity.Message;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Component
public class MessageCollector {
    private final List<Message> messageList;

    public MessageCollector() {
        this.messageList = new ArrayList<>();
    }

    public void collect(Message message) {
        messageList.add(message);
    }

    public Mono<Void> cleanup() {
        Mono<Void> chainedMono = Mono.empty();

        for(Message message : messageList) {
            chainedMono = chainedMono.and(deleteMessage(message));
        }
        messageList.clear();

        return chainedMono;
    }

    private Mono<Void> deleteMessage(Message message) {
        return message.delete().onErrorResume(error -> {
            // TODO: Log the error
            return Mono.empty();
        });
    }
}
