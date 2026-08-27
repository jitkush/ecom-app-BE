package com.ecom.foundation.auth.otpSetup.dto;

import java.time.Instant;

import com.ecom.foundation.auth.otpSetup.config.OtpContext;

public record OtpChallengeCacheEntry(
        String isd,
        String mobileE164,
        OtpContext otpContext,
        String otpDigest,
        Instant issuedAt,
        Instant expiresAt,
        Instant resendAvailableAt,
        Instant cooloffPeriod,
        int failedAttempts,
        int maxVerificationAttempt
) {}


