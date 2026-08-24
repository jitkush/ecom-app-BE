package com.ecom.foundation.common.helper;

import java.security.SecureRandom;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public final class RandomGenerator {
    private final SecureRandom secureRandom = new SecureRandom();
    public String OtpGenerator (int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("OTP length must be greater then 0");
        }

        if (length < 4 || length > 8) {
            throw new IllegalArgumentException(
                    "OTP length must be between 4 and 8 digits"
            );
        }

        int upperBound = (int) Math.pow(10, length);
        int randomNumber  = secureRandom.nextInt(upperBound);

        return String.format(Locale.ROOT, "%0" + length + "d", randomNumber);
    }
}