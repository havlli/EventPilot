package com.github.havlli.EventPilot.entity.embedtype;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Service
public class EmbedTypeService {

    private final EmbedTypeDAO embedTypeDAO;
    private final EmbedTypeSerialization serialization;

    public EmbedTypeService(EmbedTypeDAO embedTypeDAO, EmbedTypeSerialization serialization) {
        this.embedTypeDAO = embedTypeDAO;
        this.serialization = serialization;
    }

    public List<EmbedType> getAllEmbedTypes() {
        return embedTypeDAO.getAllEmbedTypes();
    }

    public void saveEmbedType(EmbedType embedType) {
        embedTypeDAO.saveEmbedType(embedType);
    }

    public Optional<EmbedType> getEmbedTypeById(Integer id) {
        return embedTypeDAO.getEmbedTypeById(id);
    }

    public HashMap<Integer, String> getDeserializedMap(EmbedType embedType) throws JsonProcessingException {
        return serialization.deserializeMap(embedType.getStructure());
    }
}
