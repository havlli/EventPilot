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
import java.util.ArrayList;
import java.util.List;

@Component
@DependsOn("restClient")
public class GlobalCommandRegistrar implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(GlobalCommandRegistrar.class);
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
    public void run(ApplicationArguments args) throws IOException{
        final JacksonResources d4jMapper = JacksonResources.create();

        final ApplicationService applicationService = restClient.getApplicationService();
        final long applicationId = restClient.getApplicationId().block();

        List<ApplicationCommandRequest> commands = new ArrayList<>();

        String locationPattern = parentFolder + "/*.json";

        try {
            for (Resource resource : pathMatcher.getResources(locationPattern)) {
                ApplicationCommandRequest request = d4jMapper.getObjectMapper()
                        .readValue(resource.getInputStream(), ApplicationCommandRequest.class);
                commands.add(request);
            }
        } catch (IOException e) {
            logger.error("Error while trying to match locationPattern[%s]".formatted(locationPattern), e);
            throw e;
        }


        applicationService.bulkOverwriteGlobalApplicationCommand(applicationId, commands)
                .doOnNext(ignore -> logger.info("Successfully registered Global Commands"))
                .doOnError(e -> logger.error("Failed to register global commands", e))
                .subscribe();
    }
}
