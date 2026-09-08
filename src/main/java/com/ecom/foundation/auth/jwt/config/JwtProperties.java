package com.ecom.foundation.auth.jwt.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(
        prefix = "app.security.jwt.otp-proof"
)
public record JwtProperties(

        @NotBlank
        String issuer,

        @NotBlank
        String audience,

        @NotNull
        Duration ttl,

        @NotNull
        Duration clockSkew,

        @NotBlank
        String kid,

        @NotNull
        Resource privateKey,

        @NotNull
        Resource publicKey
) {

    @AssertTrue(message = "OTP-proof JWT TTL must be positive")
    public boolean isTtlValid() {

        return ttl != null && !ttl.isZero() && !ttl.isNegative();
    }

    @AssertTrue(message = "JWT clock skew cannot be negative")
    public boolean isClockSkewValid() {

        return clockSkew != null && !clockSkew.isNegative();
    }
}