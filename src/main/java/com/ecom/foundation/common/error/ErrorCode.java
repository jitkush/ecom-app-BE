package com.ecom.foundation.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    INVALID_REQUEST(
            HttpStatus.BAD_REQUEST,
            "Request contains invalid data"
    ),

    VALIDATION_FAILED(
            HttpStatus.BAD_REQUEST,
            "Request validation failed"
    ),

    MALFORMED_REQUEST(
            HttpStatus.BAD_REQUEST,
            "Request body is malformed"
    ),

    AUTHENTICATION_REQUIRED(
            HttpStatus.UNAUTHORIZED,
            "Authentication is required"
    ),

    ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "You do not have permission to perform this operation"
    ),

    RESOURCE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "Requested resource was not found"
    ),

    RESOURCE_CONFLICT(
            HttpStatus.CONFLICT,
            "The requested operation conflicts with existing data"
    ),

    RATE_LIMIT_EXCEEDED(
            HttpStatus.TOO_MANY_REQUESTS,
            "Too many requests"
    ),

    OTP_CHALLENGE_INVALID(
            HttpStatus.BAD_REQUEST,
            "OTP challenge is invalid or expired"
    ),

    OTP_INVALID(
            HttpStatus.BAD_REQUEST,
            "OTP is invalid"
    ),

    OTP_RESEND_TOO_EARLY(
            HttpStatus.TOO_MANY_REQUESTS,
            "OTP cannot be resent yet"
    ),

    OTP_MAX_RETRY_EXHAUSTED(
            HttpStatus.TOO_MANY_REQUESTS,
            "Maximum OTP verification attempts exceeded. Request a new OTP"
    ),

    OTP_SERVICE_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE,
            "OTP service is temporarily unavailable"
    ),

    OTP_DELIVERY_FAILED(
            HttpStatus.SERVICE_UNAVAILABLE,
            "OTP could not be delivered"
    ),

    INTERNAL_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred"
    );

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(
            HttpStatus httpStatus,
            String message) {

        this.httpStatus = httpStatus;
        this.message = message;
    }

    public HttpStatus status() {
        return httpStatus;
    }

    public String message() {
        return message;
    }
}