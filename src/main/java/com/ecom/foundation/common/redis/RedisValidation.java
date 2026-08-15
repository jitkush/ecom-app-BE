package com.ecom.foundation.common.redis;

import java.time.Duration;
import java.util.Locale;


public final class RedisValidation {
    private RedisValidation(){};

    public static void validateKey (String key) {
        if(key == null || key.isBlank()) {
            throw new IllegalArgumentException("key is null or blank");
        }
    }

    public static <T> void validateValue (T value) {
        if(value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }
    }

    public static void validateTtl(Duration ttl) {
        if(ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl is invalid");
        }
    }

    //Redis String validator

    public static String validateAndNormaliseString(String fieldName, String value ) {
        if (value == null || value.isBlank() || value.contains(":")) {
            String errorMessage = fieldName + " cannot be empty or contain :";
            throw new IllegalArgumentException(errorMessage);
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public static String validateAndNormaliseIdentifierToken( String identifier) {
        if(identifier == null || identifier.isBlank() || identifier.contains(":")) {
            throw new IllegalArgumentException("IDENTIFIER cannot be null or contain :");
        }
        return identifier.trim();
    }

}
