package com.github.havlli.EventPilot.prompt;

import discord4j.core.object.entity.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@Scope("prototype")
public class MessageCollector {

    private static final Logger LOG = LoggerFactory.getLogger(MessageCollector.class);
    private final List<Message> messageList;

    public MessageCollector() {
        this.messageList = new ArrayList<>();
    }

    public void collect(Message message) {
        messageList.add(message);
    }

    public Flux<Void> cleanup() {
        Collections.reverse(messageList);
        return Flux.fromIterable(messageList)
                .flatMap(this::deleteMessage)
                .doFinally(onFinally -> clearMessageList());
    }

    private void clearMessageList() {
        messageList.clear();
    }

    private Mono<Void> deleteMessage(Message message) {
        return message.delete().onErrorResume(error -> {
            LOG.error("Error occurred while deleting message {}\n{}", message.getId().asString(), error.getStackTrace());
            return Mono.empty();
        });
    }

    public List<Message> getMessageList() {
        return messageList;
    }
}
