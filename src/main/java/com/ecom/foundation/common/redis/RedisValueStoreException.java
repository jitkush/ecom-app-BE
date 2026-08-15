package com.ecom.foundation.common.redis;

public class RedisValueStoreException extends RuntimeException {
    public RedisValueStoreException(
        String message,
        Throwable cause
    ) {
        super(message, cause);
    }
}
