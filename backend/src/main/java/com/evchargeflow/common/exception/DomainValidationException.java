package com.evchargeflow.common.exception;

public class DomainValidationException extends RuntimeException {
    private final String errorCode;

    public DomainValidationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
