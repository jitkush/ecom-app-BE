package com.ecom.foundation.common.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;


@Validated
@ConfigurationProperties(prefix = "app.redis.key")
public record RedisKeyProperties (

    @NotBlank
    String namespace,

    @NotBlank
    String environment
) {

}
