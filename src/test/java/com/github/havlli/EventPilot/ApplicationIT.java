package com.github.havlli.EventPilot;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ApplicationIT extends TestDatabaseContainer {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationIT.class);

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void contextLoads() {
        LOG.info("Application Context loaded with {} beans!", applicationContext.getBeanDefinitionCount());
    }

    @Test
    void migratedEventReminderLeadColumnExistsAndIsNullable() {
        assertThat(jdbcTemplate.queryForList("""
                SELECT is_nullable FROM information_schema.columns
                WHERE table_schema = current_schema()
                AND table_name = 'event' AND column_name = 'reminder_lead_minutes'
                """, String.class)).containsExactly("YES");
    }

    @Test
    void migratedSchemaHasNoAdministrativeUserTablesAndPassesJpaValidation() {
        assertThat(jdbcTemplate.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = current_schema()
                AND table_name IN ('user_role', 'role', 'users')
                """, String.class)).isEmpty();
        assertThat(applicationContext.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto"))
                .isEqualTo("validate");
        assertThat(entityManagerFactory.isOpen()).isTrue();
        assertThat(applicationContext.getEnvironment().getProperty("spring.main.web-application-type"))
                .isEqualTo("none");
        assertThat(applicationContext.containsBean("gatewayKeepAlive")).isFalse();
    }

}
