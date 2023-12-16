package com.github.havlli.EventPilot.core;

import discord4j.common.JacksonResources;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import discord4j.rest.service.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@DependsOn("restClient")
public class GlobalCommandRegistrar implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalCommandRegistrar.class);
    private final RestClient restClient;
    private final PathMatchingResourcePatternResolver pathMatcher;
    private final String parentFolder;

    public GlobalCommandRegistrar(
            RestClient restClient,
            PathMatchingResourcePatternResolver pathMatcher,
            @Value(value = "${discord.commands.folder}") String parentFolder
    ) {
        this.restClient = restClient;
        this.pathMatcher = pathMatcher;
        this.parentFolder = parentFolder;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        CommandRegistrarSpec commandRegistrarSpec = createRegistrarSpec();
        overwriteGlobalApplicationCommands(commandRegistrarSpec);
    }

    private CommandRegistrarSpec createRegistrarSpec() {
        return CommandRegistrarSpec.builder()
                .mapper(JacksonResources.create())
                .service(getApplicationService())
                .applicationId(getApplicationId())
                .build();
    }

    private ApplicationService getApplicationService() {
        return restClient.getApplicationService();
    }

    private Long getApplicationId() {
        return restClient.getApplicationId().block();
    }

    private void overwriteGlobalApplicationCommands(CommandRegistrarSpec registrarSpec) throws IOException {
        List<ApplicationCommandRequest> commands = extractApplicationCommandRequests(registrarSpec);
        registrarSpec.service()
                .bulkOverwriteGlobalApplicationCommand(registrarSpec.applicationId(), commands)
                .doOnNext(data -> LOG.info("Successfully registered Global Command [%s]".formatted(data.name())))
                .doOnError(e -> LOG.error("Failed to register global commands", e))
                .subscribe();
    }

    private List<ApplicationCommandRequest> extractApplicationCommandRequests(CommandRegistrarSpec commandRegistrarSpec) throws IOException {
        return importCommands(commandRegistrarSpec.mapper());
    }

    private List<ApplicationCommandRequest> importCommands(JacksonResources jacksonResources) throws IOException {

        return getResourceStream()
                .map(resource -> readJsonValueOrThrow(jacksonResources, resource))
                .collect(Collectors.toList());
    }

    private Stream<Resource> getResourceStream() throws IOException {
        return Arrays.stream(pathMatcher.getResources(getLocationPattern()));
    }

    protected ApplicationCommandRequest readJsonValueOrThrow(JacksonResources jacksonResources, Resource resource) throws RuntimeException {
        try {
            return readJsonValue(jacksonResources, resource);
        } catch (IOException e) {
            LOG.error("Error while trying to match locationPattern[%s]".formatted(getLocationPattern()), e);
            throw new RuntimeException(e);
        }
    }

    private String getLocationPattern() {
        return parentFolder + "/*.json";
    }

    protected ApplicationCommandRequest readJsonValue(JacksonResources jacksonResources, Resource resource) throws IOException {
        return jacksonResources.getObjectMapper()
                .readValue(resource.getInputStream(), ApplicationCommandRequest.class);
    }
}
