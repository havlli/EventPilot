package com.github.havlli.EventPilot.api;

import com.github.havlli.EventPilot.entity.guild.GuildDTO;
import com.github.havlli.EventPilot.entity.guild.GuildService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/guilds")
public class GuildController {

    private final GuildService guildService;

    public GuildController(GuildService guildService) {
        this.guildService = guildService;
    }

    @GetMapping
    public ResponseEntity<List<GuildDTO>> getAllGuilds() {
        List<GuildDTO> guildDTOList = guildService.getAllGuilds()
                .stream()
                .map(GuildDTO::fromGuild)
                .toList();

        return ResponseEntity.ok(guildDTOList);
    }
}
