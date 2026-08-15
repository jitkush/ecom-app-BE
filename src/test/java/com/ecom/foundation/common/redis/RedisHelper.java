package com.ecom.foundation.common.redis;

import java.time.Duration;

class RedisHelper {
    String validateKey (String key) {
        if(key == null || key.isBlank()) {
            throw new IllegalArgumentException("key is null or blank");
        }
        return key;
    }

    <T> void validateValue (T value) {
        if(value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }
    }

    void validateTtl(Duration ttl) {
        if(ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl is invalid");
        }
    }
}
