package com.github.havlli.EventPilot.entity.guild;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CustomizedGuildRepositoryImpl implements CustomizedGuildRepository {

    private final EntityManager entityManager;

    public CustomizedGuildRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void saveGuildIfNotExists(String id, String name) {
        String query = "INSERT INTO guild(id, name) SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM guild WHERE id = ?)";
        entityManager.createNativeQuery(query)
                .setParameter(1, id)
                .setParameter(2, name)
                .setParameter(3, id)
                .executeUpdate();
    }
}
