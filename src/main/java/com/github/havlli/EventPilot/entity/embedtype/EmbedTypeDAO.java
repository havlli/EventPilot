package com.github.havlli.EventPilot.entity.embedtype;

import java.util.List;
import java.util.Optional;

public interface EmbedTypeDAO {
    List<EmbedType> getAllEmbedTypes();
    Optional<EmbedType> getEmbedTypeById(Integer id);
    void saveEmbedType(EmbedType embedType);
}
