package com.github.havlli.EventPilot.api;

import com.github.havlli.EventPilot.exception.HttpException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;

public record ApiErrorResponse(
        String message,
        HttpStatus httpStatus,
        String path,
        String detail
) {
    public static ApiErrorResponse fromException(
            HttpException e,
            String path
    ) {
        return new ApiErrorResponse(
                e.getMessage(),
                e.getHttpStatusCode(),
                path,
                e.getClass().toString()
        );
    }

    public static ApiErrorResponse fromException(
            AuthenticationException e,
            HttpStatus httpStatus,
            String path
    ) {
        return new ApiErrorResponse(
                e.getMessage(),
                httpStatus,
                path,
                e.getClass().toString()
        );
    }

    public static ApiErrorResponse fromException(
            DataIntegrityViolationException e,
            HttpStatus httpStatus,
            String path
    ) {
        return new ApiErrorResponse(
                e.getMessage(),
                httpStatus,
                path,
                e.getClass().toString()
        );
    }
}
