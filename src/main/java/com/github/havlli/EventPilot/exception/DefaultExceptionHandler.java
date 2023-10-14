package com.github.havlli.EventPilot.exception;

import com.github.havlli.EventPilot.api.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
public class DefaultExceptionHandler {

    @ExceptionHandler(HttpException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFoundException(HttpException e, WebRequest request) {
        String requestURI = extractRequestURI(request);
        ApiErrorResponse errorResponse = ApiErrorResponse.fromException(e, requestURI);

        return ResponseEntity.status(e.getHttpStatusCode())
                .body(errorResponse);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(AuthenticationException e, WebRequest request) {
        String requestURI = extractRequestURI(request);
        ApiErrorResponse errorResponse = ApiErrorResponse.fromException(e, HttpStatus.UNAUTHORIZED, requestURI);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(errorResponse);
    }

    private String extractRequestURI(WebRequest request) {
        return ((ServletWebRequest) request).getRequest().getRequestURI();
    }
}
