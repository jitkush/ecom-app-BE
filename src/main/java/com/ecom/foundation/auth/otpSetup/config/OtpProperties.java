package com.ecom.foundation.auth.otpSetup.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import com.ecom.foundation.auth.otpSetup.config.OtpProperties.CodeLengths;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Validated
@Getter
@Setter
@ConfigurationProperties(prefix = "app.security.otp")
public class OtpProperties {

    @NotNull
    private Duration validity;

    @NotNull
    private Duration resendDelay;

    @Positive
    private int maxFailedAttempts;

    @Valid
    @NotNull
    private CodeLengths codeLengths;

    @Valid
    @NotNull
    private int maxVerificationAttempt;

    @Valid
    @NotNull
    private Duration cooloffPeriod;

    @Getter
    @Setter
    public static class CodeLengths {

        @Positive
        private int customerSignup;

        @Positive
        private int adminLogin;

        @Positive
        private int opsLogin;
    }
}