package com.ecom.foundation.auth.otpSetup.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import com.ecom.foundation.auth.otpSetup.config.OtpContext;
import com.ecom.foundation.auth.otpSetup.config.OtpHmacProperties;

@Component
public final class OtpHmacService {

    private final String ALGORITHM = "HmacSHA256";
    private final String OTP_DOMAIN = "AJ_OTP_V1";
    private final String MOBILE_DOMAIN = "AJ_OTP_MOBILE_V1";

    private final String 
    private final String keyId;

    public OtpHmacService(OtpHmacProperties properties) {

        byte[] keyBytes;

        try {
            keyBytes = Base64.getDecoder()
                    .decode(properties.getSecretBase64());

        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "OTP HMAC key is not valid Base64",
                    exception
            );
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "OTP HMAC key must contain at least 32 bytes"
            );
        }

        this.secretKey = new SecretKeySpec(
                keyBytes,
                ALGORITHM
        );

        this.keyId = properties.getKeyId();

        Arrays.fill(keyBytes, (byte) 0);
    }

    public String generateDigest(
            OtpContext otpContext,
            String isd,
            String mobile,
            String otp) {

        Objects.requireNonNull(
                otpContext,
                "otpContext is required"
        );

        Objects.requireNonNull(
                isd,
                "isd is required"
        );

        Objects.requireNonNull(
                mobile,
                "mobile is required"
        );

        Objects.requireNonNull(
                otp,
                "otp is required"
        );

        String input = String.join(
                ":",
                OTP_DOMAIN,
                otpContext.name(),
                isd,
                mobile,
                otp
        );

        return generateHmac(input);
    }

    public String generateMobileHmac(
            String isd,
            String mobile) {

        Objects.requireNonNull(
                isd,
                "isd is required"
        );

        Objects.requireNonNull(
                mobile,
                "mobile is required"
        );

        String input = String.join(
                ":",
                MOBILE_DOMAIN,
                isd,
                mobile
        );

        return generateHmac(input);
    }

    private String generateHmac(String input) {

        try {
            Mac mac = Mac.getInstance(ALGORITHM);

            mac.init(secretKey);

            byte[] digest = mac.doFinal(
                    input.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(digest);

        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Could not generate HMAC digest",
                    exception
            );
        }
    }

    public String getKeyId() {
        return keyId;
    }

    public boolean matches(
            String storedDigest,
            String candidateDigest) {

        Objects.requireNonNull(
                storedDigest,
                "storedDigest is required"
        );

        Objects.requireNonNull(
                candidateDigest,
                "candidateDigest is required"
        );

        byte[] storedBytes =
                storedDigest.getBytes(
                        StandardCharsets.US_ASCII
                );

        byte[] candidateBytes =
                candidateDigest.getBytes(
                        StandardCharsets.US_ASCII
                );

        return MessageDigest.isEqual(
                storedBytes,
                candidateBytes
        );
    }
}