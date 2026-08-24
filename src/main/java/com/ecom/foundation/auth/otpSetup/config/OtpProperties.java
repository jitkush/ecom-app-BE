package com.ecom.foundation.auth.otpSetup.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Validated
@Getter
@ConfigurationProperties(prefix = "app.security.otp")
public class OtpProperties {
    @NotBlank
    private Duration validity;

    @NotBlank
    private Duration resendDelay;

    @NotBlank
    private int maxFailedAttempts;

    @NotBlank
    private CodeLengths codeLengths;

    public static class CodeLengths {
        private int customerSignup;
        private int adminLogin;
        private int opsLogin;

        public int getCustomerSignup() {
            return customerSignup;
        }

        public int getAdminLogin() {
            return adminLogin;
        }

        public int getOpsLogin() {
            return opsLogin;
        }
    }

    public CodeLengths getCodeLengths() {
        return codeLengths;
    }
}