package com.ecom.foundation.auth.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties (prefix = "app.security.session")
public record SessionProperties(
        Duration idleTimeout,
        Duration absoluteTimeout
) {
}