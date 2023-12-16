package com.github.havlli.EventPilot.core;

import discord4j.common.JacksonResources;
import discord4j.rest.service.ApplicationService;

import java.util.Objects;

public record CommandRegistrarSpec(
        JacksonResources mapper,
        ApplicationService service,
        long applicationId
) {
    public static Builder builder() {
        return new Builder();
    }
    public static class Builder {
        private JacksonResources mapper;
        private ApplicationService service;
        private long applicationId;

        public Builder mapper(JacksonResources mapper) {
            this.mapper = mapper;
            return this;
        }

        public Builder service(ApplicationService service) {
            this.service = service;
            return this;
        }

        public Builder applicationId(long applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        public CommandRegistrarSpec build() {
            Objects.requireNonNull(mapper);
            Objects.requireNonNull(service);
            return new CommandRegistrarSpec(mapper, service, applicationId);
        }
    }
}
