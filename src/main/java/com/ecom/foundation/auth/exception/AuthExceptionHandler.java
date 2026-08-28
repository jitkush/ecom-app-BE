package com.ecom.foundation.auth.exception;

public class AuthExceptionHandler extends RuntimeException{
    public AuthExceptionHandler(String message, Throwable cause) {
        super(message, cause);
    }
}
