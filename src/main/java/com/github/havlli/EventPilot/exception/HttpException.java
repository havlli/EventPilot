package com.github.havlli.EventPilot.exception;

import org.springframework.http.HttpStatus;

public abstract class HttpException extends RuntimeException {
    private final HttpStatus httpStatusCode;

    public HttpException(String message, HttpStatus httpStatusCode) {
        super(message);
        this.httpStatusCode = httpStatusCode;
    }

    public HttpStatus getHttpStatusCode() {
        return httpStatusCode;
    }
}
