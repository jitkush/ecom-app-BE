package com.ecom.foundation.common.redis;

import static com.ecom.foundation.common.redis.RedisValidation.validateAndNormaliseString;
import static com.ecom.foundation.common.redis.RedisValidation.validateAndNormaliseIdentifierToken;

import org.springframework.stereotype.Component;

@Component
public class RedisKeyBuilder {
    private final RedisKeyProperties properties;

    public RedisKeyBuilder(RedisKeyProperties properties) {
        this.properties = properties;
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