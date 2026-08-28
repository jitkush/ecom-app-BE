package com.ecom.foundation.auth.otpSetup.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecom.foundation.auth.otpSetup.config.OtpContext;
import com.ecom.foundation.auth.otpSetup.config.OtpProperties;
import com.ecom.foundation.auth.otpSetup.dto.OtpChallengeCacheEntry;
import com.ecom.foundation.auth.otpSetup.dto.OtpChallengeResponse;
import com.ecom.foundation.auth.otpSetup.dto.OtpRequestModel;
import com.ecom.foundation.common.error.ApplicationException;
import com.ecom.foundation.common.error.ErrorCode;
import com.ecom.foundation.common.helper.RandomGenerator;
import com.ecom.foundation.common.redis.RedisKeyBuilder;
import com.ecom.foundation.common.redis.RedisValueStoreImpl;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    private static final int LOCK_STRIPES = 256;

    private final ReentrantLock[] otpLocks = createLocks();

    @Autowired
    private RandomGenerator randomGenerator;

    @Autowired
    private OtpHmacService otpHmacService;

    @Autowired
    private RedisValueStoreImpl redisValueStoreImpl;

    @Autowired
    private RedisKeyBuilder redisKeyBuilder;

    @Autowired
    private OtpProperties otpProperties;

    public OtpChallengeResponse sendOtp(OtpRequestModel request) {

        validateIdentity(request);

        log.info("Generate OTP service invoked");

        String redisKey = buildRedisKey(
                request.isd(),
                request.mobile()
        );

        return executeWithKeyLock(
                redisKey,
                () -> sendOtpWithinLock(request, redisKey)
        );
    }

    public Boolean verifyOtp(OtpRequestModel request) {

        validateIdentity(request);

        log.info("Verify OTP service invoked");

        String redisKey = buildRedisKey(
                request.isd(),
                request.mobile()
        );

        return executeWithKeyLock(redisKey,() -> verifyOtpWithinLock(request, redisKey));
    }

    private OtpChallengeResponse sendOtpWithinLock(
            OtpRequestModel request,
            String redisKey) {

        Optional<OtpChallengeCacheEntry> existingEntry = getAndVerifyRedisData(redisKey);

        int currentSendCount = existingEntry.map(OtpChallengeCacheEntry::failedAttempts).orElse(0);

        return generateNewOtp(
                request,
                redisKey,
                currentSendCount
        );
    }

    private Boolean verifyOtpWithinLock(
            OtpRequestModel request,
            String redisKey) {

        OtpChallengeCacheEntry entryData =getRedisDataFromKey(redisKey)
                                            .orElseThrow(() ->
                                                new ApplicationException(
                                                        ErrorCode.OTP_CHALLENGE_INVALID,
                                                        "OTP is invalid, already used, or not generated"
                                                )
                                            );

        if (Instant.now().isAfter(entryData.expiresAt())) {
            throw new ApplicationException(
                    ErrorCode.OTP_INVALID,
                    "OTP has expired"
            );
        }

        if (entryData.maxVerificationAttempt()
                >= otpProperties.getMaxVerificationAttempt()) {

            throw new ApplicationException(ErrorCode.OTP_MAX_RETRY_EXHAUSTED, "Maximum OTP verification attempts exhausted");
        }

        validateOtpFormat(
                request.otp(),
                entryData.otpContext()
        );

        String candidateDigest = otpHmacService.generateDigest(
                                                entryData.otpContext(),
                                                entryData.isd(),
                                                entryData.mobileE164(),
                                                request.otp()
                                            );

        boolean matched = otpHmacService.matches(
                entryData.otpDigest(),
                candidateDigest
        );

        if (matched) {
            redisValueStoreImpl.delete(redisKey);

            log.info("OTP verified successfully");

            return Boolean.TRUE;
        }

        consumeIncorrectOtp(
                redisKey,
                entryData
        );

        throw new ApplicationException(
                ErrorCode.OTP_INVALID,
                "OTP is incorrect"
        );
    }

    private Optional<OtpChallengeCacheEntry>
            getAndVerifyRedisData(String redisKey) {

        Optional<OtpChallengeCacheEntry> entry =
                getRedisDataFromKey(redisKey);

        if (entry.isEmpty()) {
            return Optional.empty();
        }

        OtpChallengeCacheEntry entryData =
                entry.get();

        Instant now = Instant.now();

        if (maxOtpSendCountReached(
                entryData.failedAttempts())) {

            if (now.isBefore(
                    entryData.cooloffPeriod())) {

                throw new ApplicationException(
                        ErrorCode.OTP_MAX_RETRY_EXHAUSTED,
                        createRetryMessage(
                                entryData.cooloffPeriod()
                        )
                );
            }

            redisValueStoreImpl.delete(redisKey);

            return Optional.empty();
        }

        if (now.isBefore(
                entryData.resendAvailableAt())) {

            throw new ApplicationException(
                    ErrorCode.OTP_RESEND_TOO_EARLY,
                    createRetryMessage(
                            entryData.resendAvailableAt()
                    )
            );
        }

        return entry;
    }

    private OtpChallengeResponse generateNewOtp(
            OtpRequestModel request,
            String redisKey,
            int currentSendCount) {

        OtpContext otpContext =
                resolveOtpContext(request.context());

        int otpDigits = OtpContext.CUSTOMER_SIGNUP
                        .equals(otpContext)
                                ? 6
                                : 8;

        String otp = randomGenerator.OtpGenerator(otpDigits);

        Instant issuedAt = Instant.now();

        Instant expiresAt = issuedAt.plus(otpProperties.getValidity());

        Instant resendAvailableAt = issuedAt.plus(otpProperties.getResendDelay());

        Instant cooloffPeriod = issuedAt.plus(otpProperties.getCooloffPeriod());

        int updatedSendCount = currentSendCount + 1;

        String otpDigest = otpHmacService.generateDigest(
                            otpContext,
                            request.isd(),
                            request.mobile(),
                            otp
                        );

        OtpChallengeCacheEntry newEntry =
                new OtpChallengeCacheEntry(
                        request.isd(),
                        request.mobile(),
                        otpContext,
                        otpDigest,
                        issuedAt,
                        expiresAt,
                        resendAvailableAt,
                        cooloffPeriod,
                        updatedSendCount,
                        0
                );

        Duration redisTtl = Duration.between(
                Instant.now(),
                cooloffPeriod
        );

        if (redisTtl.isNegative()
                || redisTtl.isZero()) {

            throw new IllegalStateException(
                    "OTP cooldown period must be positive"
            );
        }

        redisValueStoreImpl.save(
                redisKey,
                newEntry,
                redisTtl
        );

        log.info(
                "OTP generated context={} sendCount={}",
                otpContext,
                updatedSendCount
        );

        log.debug(
                "LOCAL TEST ONLY: generated OTP={}",
                otp
        );

        return new OtpChallengeResponse(
                expiresAt,
                resendAvailableAt
        );
    }

    private void consumeIncorrectOtp(
            String redisKey,
            OtpChallengeCacheEntry entryData) {

        int currentAttempts = entryData.maxVerificationAttempt();

        int maxAttempts = otpProperties.getMaxVerificationAttempt();

        if (currentAttempts >= maxAttempts) {
            throw new ApplicationException(
                    ErrorCode.OTP_MAX_RETRY_EXHAUSTED,
                    "Maximum OTP verification attempts exhausted"
            );
        }

        int updatedAttempts =
                currentAttempts + 1;

        Duration remainingTtl = Duration.between(Instant.now(),entryData.cooloffPeriod());

        if (remainingTtl.isNegative()
                || remainingTtl.isZero()) {

            redisValueStoreImpl.delete(redisKey);

            throw new ApplicationException(
                    ErrorCode.OTP_CHALLENGE_INVALID,
                    "OTP challenge has expired"
            );
        }

        OtpChallengeCacheEntry updatedEntry =
                new OtpChallengeCacheEntry(
                        entryData.isd(),
                        entryData.mobileE164(),
                        entryData.otpContext(),
                        entryData.otpDigest(),
                        entryData.issuedAt(),
                        entryData.expiresAt(),
                        entryData.resendAvailableAt(),
                        entryData.cooloffPeriod(),
                        entryData.failedAttempts(),
                        updatedAttempts
                );

        redisValueStoreImpl.save(
                redisKey,
                updatedEntry,
                remainingTtl
        );

        if (updatedAttempts >= maxAttempts) {
            throw new ApplicationException(
                    ErrorCode.OTP_MAX_RETRY_EXHAUSTED,
                    "Maximum OTP verification attempts exhausted"
            );
        }
    }

    private Optional<OtpChallengeCacheEntry>
            getRedisDataFromKey(String redisKey) {

        return redisValueStoreImpl.find(
                redisKey,
                OtpChallengeCacheEntry.class
        );
    }

    private String buildRedisKey(
            String isd,
            String mobile) {

        String mobileHmac = otpHmacService.generateMobileHmac(isd,mobile);

        return redisKeyBuilder.build(
                "OTP",
                "mobileHmac",
                mobileHmac
        );
    }

    private OtpContext resolveOtpContext(
            String suppliedContext) {

        if (suppliedContext == null
                || suppliedContext.isBlank()) {

            return OtpContext.CUSTOMER_SIGNUP;
        }

        try {
            return OtpContext.valueOf(
                    suppliedContext
                            .trim()
                            .toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new ApplicationException(
                    ErrorCode.OTP_CHALLENGE_INVALID,
                    "Invalid OTP context"
            );
        }
    }

    private void validateIdentity(
            OtpRequestModel request) {

        if (request == null) {
            throw new ApplicationException(
                    ErrorCode.OTP_CHALLENGE_INVALID,
                    "OTP request is required"
            );
        }

        if (request.isd() == null
                || request.isd().isBlank()
                || request.mobile() == null
                || request.mobile().isBlank()) {

            throw new ApplicationException(
                    ErrorCode.OTP_CHALLENGE_INVALID,
                    "ISD code and mobile number are required"
            );
        }
    }

    private void validateOtpFormat(
            String otp,
            OtpContext otpContext) {

        int expectedDigits = OtpContext.CUSTOMER_SIGNUP.equals(otpContext) ? 6 : 8;

        String pattern = "[0-9]{" + expectedDigits + "}";

        if (otp == null || !otp.matches(pattern)) {
            throw new ApplicationException(
                    ErrorCode.OTP_INVALID,
                    "OTP format is invalid"
            );
        }
    }

    private boolean maxOtpSendCountReached(int currentSendCount) {

        return currentSendCount >= otpProperties.getMaxFailedAttempts();
    }

    private String createRetryMessage(Instant retryAt) {

        Duration remaining = Duration.between(
                Instant.now(),
                retryAt
        );

        if (remaining.isNegative()) {
            remaining = Duration.ZERO;
        }

        return "Please retry after %s".formatted(remaining);
    }

    private <T> T executeWithKeyLock(
            String redisKey,
            Supplier<T> operation) {

        int lockIndex = Math.floorMod(redisKey.hashCode(),LOCK_STRIPES);

        ReentrantLock lock = otpLocks[lockIndex];

        lock.lock();

        try {
            return operation.get();
        } finally {
            lock.unlock();
        }
    }

    private static ReentrantLock[] createLocks() {

        ReentrantLock[] locks = new ReentrantLock[LOCK_STRIPES];

        for (int index = 0; index < LOCK_STRIPES; index++) {
            locks[index] = new ReentrantLock();
        }
        return locks;
    }
}