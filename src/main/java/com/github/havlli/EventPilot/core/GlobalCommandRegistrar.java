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
    public void run(ApplicationArguments args) throws IOException{
        final JacksonResources mapper = JacksonResources.create();
        final ApplicationService service = getApplicationService();
        final long applicationId = getApplicationId();

        List<ApplicationCommandRequest> commands = extractApplicationCommandRequstList(mapper);
        overwriteGlobalApplicationCommands(service, applicationId, commands);
    }

    private String getLocationPattern() {
        return parentFolder + "/*.json";
    }

    private List<ApplicationCommandRequest> extractApplicationCommandRequstList(JacksonResources jacksonResources) throws IOException {
        List<ApplicationCommandRequest> commands = new ArrayList<>();
        try {
            for (Resource resource : pathMatcher.getResources(getLocationPattern())) {
                ApplicationCommandRequest request = jacksonResources.getObjectMapper()
                        .readValue(resource.getInputStream(), ApplicationCommandRequest.class);
                commands.add(request);
            }
        } catch (IOException e) {
            LOG.error("Error while trying to match locationPattern[%s]".formatted(getLocationPattern()), e);
            throw e;
        }

        return commands;
    }

    private static void overwriteGlobalApplicationCommands(ApplicationService applicationService, long applicationId, List<ApplicationCommandRequest> commands) {
        applicationService.bulkOverwriteGlobalApplicationCommand(applicationId, commands)
                .doOnNext(data -> LOG.info("Successfully registered Global Command [%s]".formatted(data.name())))
                .doOnError(e -> LOG.error("Failed to register global commands", e))
                .subscribe();
    }

    private Long getApplicationId() {
        return restClient.getApplicationId().block();
    }

    private ApplicationService getApplicationService() {
        return restClient.getApplicationService();
    }
}
