package com.github.havlli.EventPilot.api.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    @Test
    void corsConfigurationSource_UsesExplicitApiCorsPolicy() {
        ApiSecurityProperties securityProperties = new ApiSecurityProperties(
                new ApiSecurityProperties.Jwt("secret"),
                new ApiSecurityProperties.Cors(
                        List.of("http://localhost:3000", "http://localhost:5173"),
                        List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"),
                        List.of("Authorization", "Content-Type"),
                        List.of("Authorization")
                )
        );
        CorsConfig underTest = new CorsConfig(securityProperties);

        CorsConfigurationSource source = underTest.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");

        CorsConfiguration actual = source.getCorsConfiguration(request);

        assertThat(actual).isNotNull();
        assertThat(actual.getAllowedOrigins()).containsExactly("http://localhost:3000", "http://localhost:5173");
        assertThat(actual.getAllowedOrigins()).doesNotContain("*");
        assertThat(actual.getAllowedMethods()).containsExactly("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(actual.getAllowedHeaders()).containsExactly("Authorization", "Content-Type");
        assertThat(actual.getExposedHeaders()).containsExactly("Authorization");
    }
}
