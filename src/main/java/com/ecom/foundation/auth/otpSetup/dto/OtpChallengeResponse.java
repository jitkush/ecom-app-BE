package com.ecom.foundation.auth.otpSetup.dto;

import java.time.Instant;
import java.util.UUID;

public record OtpChallengeResponse(
        Instant expiresAt,
        Instant resendAvailableAt
) {}
