package com.github.havlli.EventPilot.entity.embedtype;

public record EmbedTypeDTO(String name, String structure) {
    public static EmbedTypeDTO fromEmbedType(EmbedType embedType) {
        return new EmbedTypeDTO(embedType.getName(), embedType.getStructure());
    }
}
