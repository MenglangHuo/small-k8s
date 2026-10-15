package com.menglang.ticket.domain.exception;

public class UnauthorizedClientException extends RuntimeException {
    public UnauthorizedClientException(String message) {
        super(message);
    }
}
