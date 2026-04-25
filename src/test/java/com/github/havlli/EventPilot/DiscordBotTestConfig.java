package com.github.havlli.EventPilot;

import io.github.cdimascio.dotenv.Dotenv;

public abstract class DiscordBotTestConfig {
    protected String getToken() {
        String token = System.getenv("TEST_DISCORD_BOT_TOKEN");
        if (token != null && !token.isBlank()) {
            return token;
        }

        return Dotenv.configure()
                .filename(".env")
                .ignoreIfMissing()
                .load()
                .get("TEST_DISCORD_BOT_TOKEN");
    }
}
