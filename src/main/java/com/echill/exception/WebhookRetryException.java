package com.echill.exception;

public class WebhookRetryException extends RuntimeException {
    public WebhookRetryException(String message) {
        super(message);
    }
    public WebhookRetryException(String message, Throwable cause) {
        super(message, cause);
    }
}
