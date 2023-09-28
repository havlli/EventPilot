package com.github.havlli.EventPilot.session;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Testcontainers
class SessionStorageRedisIntegrationTest {

    @Container
    public static RedisContainer redisContainer = new RedisContainer("7.2.1-alpine");

    @Autowired
    private SessionStorage underTest;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @DynamicPropertySource
    private static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", () -> redisContainer.getHost());
        registry.add("spring.data.redis.port", () -> redisContainer.getFirstMappedPort());
        System.out.printf("Redis host: %s, port: %d", redisContainer.getHost(), redisContainer.getFirstMappedPort());
    }

    @BeforeEach
    void setUp() {
        redisTemplate.delete(redisTemplate.keys("*"));
    }

    @Test
    void testConnection() {
        assertTrue(redisContainer.isEnabled());
    }

    @Test
    void save_WillSaveToRedis() {
        // Act
        underTest.save("test", "test");

        // Assert
        assertTrue(redisTemplate.hasKey("test"));
    }

    @Test
    void exists_WillReturnTrue_WhenKeyExists() {
        // Arrange
        redisTemplate.opsForValue().set("test", "test");

        // Act
        boolean actual = underTest.exists("test");

        // Assert
        assertTrue(actual);
    }

    @Test
    void exists_WillReturnFalse_WhenKeyDoesNotExist() {
        // Act
        boolean actual = underTest.exists("test");

        // Assert
        assertFalse(actual);
    }

    @Test
    void remove_WillRemoveFromRedis() {
        // Arrange
        redisTemplate.opsForValue().set("test", "test");

        // Act
        underTest.remove("test");

        // Assert
        assertFalse(redisTemplate.hasKey("test"));
    }

    @Test
    void clear_WillClearRedis() {
        // Arrange
        redisTemplate.opsForValue().set("test1", "test1");
        redisTemplate.opsForValue().set("test2","test2");

        // Act
        underTest.clear();

        // Assert
        assertFalse(redisTemplate.hasKey("test1"));
        assertFalse(redisTemplate.hasKey("test2"));
    }
}