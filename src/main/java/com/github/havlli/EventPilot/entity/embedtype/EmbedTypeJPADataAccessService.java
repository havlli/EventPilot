package com.github.havlli.EventPilot.entity.embedtype;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class EmbedTypeJPADataAccessService implements EmbedTypeDAO {

    private final EmbedTypeRepository embedTypeRepository;

    public EmbedTypeJPADataAccessService(EmbedTypeRepository embedTypeRepository) {
        this.embedTypeRepository = embedTypeRepository;
    }

    @Override
    public List<EmbedType> getAllEmbedTypes() {
        return embedTypeRepository.findAll();
    }

    @Override
    public Optional<EmbedType> getEmbedTypeById(Long id) {
        return embedTypeRepository.findById(id);
    }

    @Override
    public void saveEmbedType(EmbedType embedType) {
        embedTypeRepository.save(embedType);
    }

    @Override
    public boolean existsEmbedTypeById(Long id) {
        return embedTypeRepository.existsById(id);
    }
}
