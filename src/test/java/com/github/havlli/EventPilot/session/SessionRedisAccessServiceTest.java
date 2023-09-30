package com.github.havlli.EventPilot.session;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Set;

import static org.mockito.Mockito.*;

class SessionRedisAccessServiceTest {

    private AutoCloseable autoCloseable;
    private SessionRedisAccessService underTest;
    @Mock
    private StringRedisTemplate redisTemplateMock;
    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new SessionRedisAccessService(redisTemplateMock);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void save() {
        // Arrange
        String key = "key";
        String value = "value";
        ValueOperations<String, String> valueOperationsMock = mock(ValueOperations.class);
        when(redisTemplateMock.opsForValue()).thenReturn(valueOperationsMock);

        // Act
        underTest.save(key, value);

        // Assert
        verify(valueOperationsMock, only()).set(key, value);
    }

    @Test
    void exists() {
        // Act
        underTest.exists("key");

        // Assert
        verify(redisTemplateMock, only()).hasKey("key");
    }

    @Test
    void remove() {
        // Act
        underTest.remove("key");

        // Assert
        verify(redisTemplateMock, only()).delete("key");
    }

    @Test
    void clear() {
        // Arrange
        when(redisTemplateMock.keys("*")).thenReturn(Set.of());

        // Act
        underTest.clear();

        // Assert
        verify(redisTemplateMock, times(1)).delete(anySet());
    }
}