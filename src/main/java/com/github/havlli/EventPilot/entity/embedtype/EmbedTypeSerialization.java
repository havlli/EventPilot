package com.github.havlli.EventPilot.entity.embedtype;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class EmbedTypeSerialization {

    private static final HashMap<Integer, String> DEFAULT_MAP = new HashMap<>(Map.of(
            -1, "Absence",
            -2, "Late",
            -3, "Tentative",
            1, "Tank",
            2, "Melee",
            3, "Ranged",
            4, "Healer",
            5, "Support"
    ));
    private final ObjectMapper objectMapper;

    public EmbedTypeSerialization(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serializeDefaultMap() throws JsonProcessingException {
        return serializeMap(DEFAULT_MAP);
    }

    public String serializeMap(HashMap<Integer, String> map) throws JsonProcessingException {
        return objectMapper.writeValueAsString(map);
    }

    public HashMap<Integer, String> deserializeMap(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, HashMap.class);
    }
}
