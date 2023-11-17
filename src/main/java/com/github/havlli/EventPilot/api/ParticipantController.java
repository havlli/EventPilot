package com.github.havlli.EventPilot.api;

import com.github.havlli.EventPilot.entity.participant.ParticipantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/participants")
public class ParticipantController {

    private final ParticipantService participantService;
    public ParticipantController(ParticipantService participantService) {
        this.participantService = participantService;
    }

    @DeleteMapping("/{participantId}")
    public ResponseEntity<?> deleteParticipant(@PathVariable Long participantId) {
        if (participantService.removeParticipantFromDiscordEvent(participantId)) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
