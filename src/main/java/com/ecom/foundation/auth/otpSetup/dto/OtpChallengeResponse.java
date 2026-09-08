package com.ecom.foundation.auth.otpSetup.dto;

import java.time.Instant;

public record OtpChallengeResponse(
        Instant expiresAt,
        Instant resendAvailableAt
) {}
