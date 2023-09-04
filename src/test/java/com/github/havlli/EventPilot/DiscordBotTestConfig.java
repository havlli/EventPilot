package com.github.havlli.EventPilot;

import io.github.cdimascio.dotenv.Dotenv;

public abstract class DiscordBotTestConfig {
    protected String getToken() {
        return Dotenv.configure()
                .filename(".env")
                .load()
                .get("TEST_DISCORD_BOT_TOKEN");
    }
}
