package com.github.havlli.EventPilot;

import com.github.havlli.EventPilot.entity.guild.Guild;
import com.github.havlli.EventPilot.entity.guild.GuildService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	CommandLineRunner runner(GuildService guildService) {
		return args -> {
			Guild guild = new Guild("1","testsss");
			guildService.saveGuild(guild);
		};
	}

}
