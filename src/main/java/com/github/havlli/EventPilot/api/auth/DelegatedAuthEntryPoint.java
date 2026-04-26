package com.github.havlli.EventPilot.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.havlli.EventPilot.api.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class DelegatedAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public DelegatedAuthEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        ApiErrorResponse errorResponse = ApiErrorResponse.fromException(
                authException,
                HttpStatus.UNAUTHORIZED,
                request.getRequestURI()
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.copy()
                .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
                .writeValue(response.getOutputStream(), errorResponse);
    }
}
