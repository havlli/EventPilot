package com.github.havlli.EventPilot.entity.embedtype;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmbedTypeService {

    private final EmbedTypeDAO embedTypeDAO;

    public EmbedTypeService(EmbedTypeDAO embedTypeDAO) {
        this.embedTypeDAO = embedTypeDAO;
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
}
