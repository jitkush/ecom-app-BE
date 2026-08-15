package com.ecom.foundation.common.redis;

import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class RedisKeyBuilder {
    private final RedisKeyProperties properties;

    public RedisKeyBuilder(RedisKeyProperties properties) {
        this.properties = properties;
    }

    private String validateAndNormaliseString(String fieldName, String value ) {
        if (value == null || value.isBlank() || value.contains(":")) {
            String errorMessage = fieldName + " cannot be empty or contain :";
            throw new IllegalArgumentException(errorMessage);
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String validateAndNormaliseIdentifierToken( String identifier) {
        if(identifier == null || identifier.isBlank() || identifier.contains(":")) {
            throw new IllegalArgumentException("IDENTIFIER cannot be null or contain :");
        }
        return identifier.trim();
    }

    public String build(
        String module,
        String resource,
        String identifierToken
    ) {
        String normalizedModule = validateAndNormaliseString("module", module);
        String normalizedResource = validateAndNormaliseString("resource", resource);
        String normalizedIndetifier = validateAndNormaliseIdentifierToken(identifierToken);

        return String.join(
            ":",
            properties.namespace(),
            properties.environment(),
            normalizedModule,
            normalizedResource,
            normalizedIndetifier
        );
    }
}