package com.github.havlli.EventPilot.generator;

import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class EmbedGenerator {

    public EmbedCreateSpec generatePreview(EmbedPreviewable embedPreviewable) {
        List<EmbedCreateFields.Field> fields = new ArrayList<>();
        embedPreviewable.previewFields().forEach((key, value) -> {
            String name = key.substring(0,1).toUpperCase().concat(key.substring(1));

            if (value != null)
                fields.add(EmbedCreateFields.Field.of(name, value, false));
        });

        return EmbedCreateSpec.builder()
                .addAllFields(fields)
                .build();
    }
}
