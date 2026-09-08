package com.ecom.foundation.auth.jwt.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import com.ecom.foundation.auth.jwt.config.JwtProperties;
import com.ecom.foundation.auth.otpSetup.config.OtpContext;
import com.ecom.foundation.common.error.ApplicationException;
import com.ecom.foundation.common.error.ErrorCode;
import com.ecom.foundation.common.redis.RedisKeyBuilder;
import com.ecom.foundation.common.redis.RedisValueStoreException;
import com.ecom.foundation.common.redis.RedisValuestore;

@Service
public class JwtService {

    private static final String TOKEN_TYPE = "otp-proof+jwt";
    private static final String TOKEN_USE = "OTP_PROOF";
    private static final String JTI_MARKER = "UNUSED";
    private static final Pattern ISD_PATTERN = Pattern.compile("^91$");
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^[6-9]\\d{9}$");

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final JwtProperties jwtProperties;
    private final RedisValuestore redisValueStore;
    private final RedisKeyBuilder redisKeyBuilder;
    private final Clock jwtClock;

    public JwtService(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder, JwtProperties jwtProperties, RedisValuestore redisValueStore, RedisKeyBuilder redisKeyBuilder, Clock jwtClock) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.jwtProperties = jwtProperties;
        this.redisValueStore = redisValueStore;
        this.redisKeyBuilder = redisKeyBuilder;
        this.jwtClock = jwtClock;
    }

    public String generateJwt(String isd, String mobile, OtpContext context) {
        validateGenerationInput(isd, mobile, context);

        Instant issuedAt = jwtClock.instant();
        Instant expiresAt = issuedAt.plus(jwtProperties.ttl());
        String jti = UUID.randomUUID().toString();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(jwtProperties.kid()).type(TOKEN_TYPE).build();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .audience(List.of(jwtProperties.audience()))
                .issuedAt(issuedAt)
                .notBefore(issuedAt)
                .expiresAt(expiresAt)
                .id(jti)
                .claim("isd", isd)
                .claim("mobile", mobile)
                .claim("token_use", TOKEN_USE)
                .claim("purpose", context.name())
                .build();

        String token = encode(header, claims);
        Duration redisTtl = jwtProperties.ttl().plus(jwtProperties.clockSkew());

        registerJti(jti, redisTtl);

        return token;
    }

    public void validateAndConsumeJwt(String token, String expectedIsd, String expectedMobile, OtpContext expectedContext) {
        validateConsumptionInput(token, expectedIsd, expectedMobile, expectedContext);

        Jwt jwt = decode(token);

        validateClaims(jwt, expectedIsd, expectedMobile, expectedContext);
        consumeJti(jwt.getId());
    }

    private String encode(JwsHeader header, JwtClaimsSet claims) {
        try {
            JwtEncoderParameters parameters = JwtEncoderParameters.from(header, claims);
            return jwtEncoder.encode(parameters).getTokenValue();
        } catch (JwtEncodingException exception) {
            throw new ApplicationException(ErrorCode.OTP_PROOF_SERVICE_UNAVAILABLE, "OTP proof could not be generated", exception);
        }
    }

    private Jwt decode(String token) {
        try {
            return jwtDecoder.decode(token);
        } catch (JwtException exception) {
            throw invalidJwt();
        }
    }

    private void validateClaims(Jwt jwt, String expectedIsd, String expectedMobile, OtpContext expectedContext) {
        if (!TOKEN_TYPE.equals(jwt.getHeaders().get("typ"))) {
            throw invalidJwt();
        }

        if (!jwtProperties.kid().equals(jwt.getHeaders().get("kid"))) {
            throw invalidJwt();
        }

        if (!TOKEN_USE.equals(jwt.getClaimAsString("token_use"))) {
            throw invalidJwt();
        }

        if (jwt.getAudience() == null || !jwt.getAudience().contains(jwtProperties.audience())) {
            throw invalidJwt();
        }

        if (!expectedIsd.equals(jwt.getClaimAsString("isd"))) {
            throw invalidJwt();
        }

        if (!expectedMobile.equals(jwt.getClaimAsString("mobile"))) {
            throw invalidJwt();
        }

        if (!expectedContext.name().equals(jwt.getClaimAsString("purpose"))) {
            throw invalidJwt();
        }

        validateTimeClaims(jwt);
        validateJti(jwt.getId());
    }

    private void validateTimeClaims(Jwt jwt) {
        Instant issuedAt = jwt.getIssuedAt();
        Instant expiresAt = jwt.getExpiresAt();

        if (issuedAt == null || expiresAt == null) {
            throw invalidJwt();
        }

        Duration tokenLifetime = Duration.between(issuedAt, expiresAt);

        if (tokenLifetime.isZero() || tokenLifetime.isNegative() || tokenLifetime.compareTo(jwtProperties.ttl()) > 0) {
            throw invalidJwt();
        }

        if (issuedAt.isAfter(jwtClock.instant().plus(jwtProperties.clockSkew()))) {
            throw invalidJwt();
        }
    }

    private void registerJti(String jti, Duration ttl) {
        String redisKey = buildJtiRedisKey(jti);

        try {
            redisValueStore.save(redisKey, JTI_MARKER, ttl);
        } catch (RedisValueStoreException exception) {
            throw new ApplicationException(ErrorCode.OTP_PROOF_SERVICE_UNAVAILABLE, exception);
        }
    }

    private void consumeJti(String jti) {
        validateJti(jti);

        String redisKey = buildJtiRedisKey(jti);

        try {
            boolean consumed = redisValueStore.delete(redisKey);

            if (!consumed) {
                throw invalidJwt();
            }
        } catch (RedisValueStoreException exception) {
            throw new ApplicationException(ErrorCode.OTP_PROOF_SERVICE_UNAVAILABLE, exception);
        }
    }

    private String buildJtiRedisKey(String jti) {
        return redisKeyBuilder.build("auth", "otp-proof-jti", jti);
    }

    private void validateGenerationInput(String isd, String mobile, OtpContext context) {
        if (!isValidIdentity(isd, mobile)) {
            throw new IllegalArgumentException("Verified mobile identity is invalid");
        }

        if (context == null) {
            throw new IllegalArgumentException("OTP context is required");
        }
    }

    private void validateConsumptionInput(String token, String expectedIsd, String expectedMobile, OtpContext expectedContext) {
        if (token == null || token.isBlank() || !isValidIdentity(expectedIsd, expectedMobile) || expectedContext == null) {
            throw invalidJwt();
        }
    }

    private boolean isValidIdentity(String isd, String mobile) {
        return isd != null && mobile != null && ISD_PATTERN.matcher(isd).matches() && MOBILE_PATTERN.matcher(mobile).matches();
    }

    private void validateJti(String jti) {
        if (jti == null || jti.length() != 36) {
            throw invalidJwt();
        }

        try {
            UUID.fromString(jti);
        } catch (IllegalArgumentException exception) {
            throw invalidJwt();
        }
    }

    private ApplicationException invalidJwt() {
        return new ApplicationException(ErrorCode.JWT_INVALID);
    }
}