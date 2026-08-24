package com.ecom.foundation.auth.otpSetup.config;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security.otp.hmac")
public final class OtpHmacProperties {

    @NotBlank
    private final String keyId;

    @NotBlank
    private final String secretBase64;

    public OtpHmacProperties(String keyId, String secretBase64) {
        this.keyId = keyId;
        this.secretBase64 = secretBase64;
    }

    public String getKeyId() {
        return keyId;
    }


    public String getSecretBase64() {
        return secretBase64;
    }

    @Override
    public String toString() {
        return "OtpHmacProperties{keyId='%s', secretBase64='[REDACTED]'}"
                .formatted(keyId);
    }
}