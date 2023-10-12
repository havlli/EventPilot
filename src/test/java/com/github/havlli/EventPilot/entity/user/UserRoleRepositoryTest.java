package com.github.havlli.EventPilot.entity.user;

import com.github.havlli.EventPilot.TestDatabaseContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class UserRoleRepositoryTest extends TestDatabaseContainer {

    public static final Logger LOG = LoggerFactory.getLogger(UserRoleRepositoryTest.class);
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired UserRoleRepository underTest;
    @BeforeEach
    void setUp() {
        LOG.info("Number of beans initialized { {} }", applicationContext.getBeanDefinitionCount());
    }

    @Test
    void findByRole_ReturnsOptionalRole_WhenRoleExists() {
        // Arrange
        UserRole.Role expectedRole = UserRole.Role.USER;

        // Act
        Optional<UserRole> actualRole = underTest.findByRole(expectedRole);

        // Assert
        assertThat(actualRole).hasValueSatisfying(actual -> assertThat(actual.getRole()).isEqualTo(expectedRole));
    }

    @Test
    void findByRole_ReturnsEmptyOptional_WhenRoleNotExists() {
        // Arrange
        underTest.deleteAll();

        // Act
        Optional<UserRole> actualRole = underTest.findByRole(UserRole.Role.ADMIN);

        // Assert
        assertThat(actualRole).isEmpty();
    }
}