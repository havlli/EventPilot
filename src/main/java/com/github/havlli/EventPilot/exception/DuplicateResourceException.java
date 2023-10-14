package com.github.havlli.EventPilot.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends HttpException {
    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
