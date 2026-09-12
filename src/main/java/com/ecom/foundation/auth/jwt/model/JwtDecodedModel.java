package com.ecom.foundation.auth.jwt.model;

import java.time.Instant;

import com.ecom.foundation.auth.otpSetup.config.OtpContext;

public record JwtDecodedModel(
        String jti,
        String isd,
        String mobile,
        OtpContext purpose,
        Instant expiresAt
) {
}