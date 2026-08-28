package com.ecom.foundation.auth.Service;

import java.time.Instant;

import org.springframework.stereotype.Service;

@Service
public class OtpChallengeService {
    public String getOtp(String mobile, String purpose, String isdCode, String attempt, Instant lastRetry, Instant CreatedAt) {
        return "Otp";
    }
}
