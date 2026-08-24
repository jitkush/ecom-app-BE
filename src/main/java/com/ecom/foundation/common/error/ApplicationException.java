package com.ecom.foundation.common.error;

import java.util.Objects;

public final class ApplicationException extends RuntimeException {

    private final ErrorCode errorCode;

    public ApplicationException(ErrorCode errorCode) {
        this(errorCode, null, null);
    }

    public ApplicationException(
            ErrorCode errorCode,
            String message) {

        this(errorCode, message, null);
    }

    public ApplicationException(
            ErrorCode errorCode,
            Throwable cause) {

        this(errorCode, null, cause);
    }

    public ApplicationException(
            ErrorCode errorCode,
            String message,
            Throwable cause) {

        super(
                resolveMessage(errorCode, message),
                cause
        );

        this.errorCode = Objects.requireNonNull(
                errorCode,
                "errorCode is required"
        );
    }

    private static String resolveMessage(
            ErrorCode errorCode,
            String suppliedMessage) {

        Objects.requireNonNull(
                errorCode,
                "errorCode is required"
        );

        if (suppliedMessage == null || suppliedMessage.isBlank()) {
            return errorCode.message();
        }

        return suppliedMessage;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getResponseMessage() {
        return getMessage();
    }
}