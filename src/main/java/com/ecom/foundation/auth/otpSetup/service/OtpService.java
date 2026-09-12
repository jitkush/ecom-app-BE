package com.ecom.foundation.auth.otpSetup.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecom.foundation.auth.jwt.service.JwtService;
import com.ecom.foundation.auth.otpSetup.config.OtpContext;
import com.ecom.foundation.auth.otpSetup.config.OtpProperties;
import com.ecom.foundation.auth.otpSetup.dto.OtpChallengeCacheEntry;
import com.ecom.foundation.auth.otpSetup.dto.OtpChallengeResponse;
import com.ecom.foundation.auth.otpSetup.dto.OtpRequestModel;
import com.ecom.foundation.auth.otpSetup.store.OtpAtomicStore;
import com.ecom.foundation.auth.otpSetup.store.OtpAtomicStore.AtomicResult;
import com.ecom.foundation.auth.otpSetup.store.OtpAtomicStore.VersionedOtpChallenge;
import com.ecom.foundation.common.error.ApplicationException;
import com.ecom.foundation.common.error.ErrorCode;
import com.ecom.foundation.common.helper.RandomGenerator;
import com.ecom.foundation.common.redis.RedisKeyBuilder;

@Service
public class OtpService {

        private static final Logger log = LoggerFactory.getLogger(OtpService.class);

        @Autowired
        private RandomGenerator randomGenerator;

        @Autowired
        private OtpHmacService otpHmacService;

        @Autowired
        private OtpAtomicStore otpAtomicStore;

        @Autowired
        private RedisKeyBuilder redisKeyBuilder;

        @Autowired
        private OtpProperties otpProperties;

        @Autowired
        private JwtService jwtService;

        public OtpChallengeResponse sendOtp(OtpRequestModel request) {
                validateIdentity(request);

                String redisKey = buildRedisKey(request.isd(), request.mobile());

                Optional<VersionedOtpChallenge> existingChallenge = otpAtomicStore.find(redisKey);

                if (existingChallenge.isPresent()) {
                        validateChallengeBeforeSend(existingChallenge.get().cacheEntry());
                }

                return generateNewOtp(redisKey, request, existingChallenge.orElse(null));
        }

        public String verifyOtp(OtpRequestModel request) {

                validateIdentity(request);

                String redisKey = buildRedisKey(request.isd(), request.mobile());

                VersionedOtpChallenge versionedChallenge = otpAtomicStore.find(redisKey)
                                .orElseThrow(() -> new ApplicationException(ErrorCode.OTP_CHALLENGE_INVALID, "OTP is invalid, expired, or already consumed"));

                OtpChallengeCacheEntry cacheEntry = versionedChallenge.cacheEntry();

                Instant now = Instant.now();

                if (!now.isBefore(cacheEntry.expiresAt())) {
                        throw new ApplicationException(ErrorCode.OTP_INVALID, "OTP has expired");
                }

                if (cacheEntry.maxVerificationAttempt() >= otpProperties.getMaxVerificationAttempt()) {

                        throw new ApplicationException(ErrorCode.OTP_MAX_RETRY_EXHAUSTED, "Maximum OTP verification attempts reached");
                }

                List<String> otpDigests = cacheEntry.otpDigest();

                if (otpDigests == null || otpDigests.isEmpty()) {
                        throw new ApplicationException(ErrorCode.OTP_CHALLENGE_INVALID, "OTP challenge contains no valid OTP");
                }

                String currentOtpDigest = otpDigests.get(otpDigests.size() - 1);

                String candidateDigest = otpHmacService.generateDigest(cacheEntry.otpContext(), cacheEntry.isd(), cacheEntry.mobileE164(), request.otp());

                boolean matched = otpHmacService.matches(currentOtpDigest, candidateDigest);

                if (!matched) {
                        consumeIncorrectOtp(redisKey, versionedChallenge);

                        throw new ApplicationException(ErrorCode.OTP_INVALID, "OTP is incorrect");
                }

                AtomicResult deleteResult = otpAtomicStore.deleteIfUnchanged(redisKey, versionedChallenge.comparisonToken());

                if (deleteResult != AtomicResult.APPLIED) {
                        throw new ApplicationException(ErrorCode.OTP_CHALLENGE_INVALID, "OTP challenge changed or was already consumed");
                }

                String proofJwt = jwtService.generateJwt(cacheEntry.isd(), cacheEntry.mobileE164(), cacheEntry.otpContext());

                log.info("OTP verified and proof JWT generated context={}", cacheEntry.otpContext());

                return proofJwt;
        }

        private OtpChallengeResponse generateNewOtp(String redisKey, OtpRequestModel request, VersionedOtpChallenge existingChallenge) {
                Instant issuedAt = Instant.now();
                OtpChallengeCacheEntry existingEntry = existingChallenge == null ? null : existingChallenge.cacheEntry();

                OtpContext otpContext = existingEntry == null ? resolveOtpContext(request.context()) : existingEntry.otpContext();

                String isd = existingEntry == null ? request.isd() : existingEntry.isd();

                String mobile = existingEntry == null ? request.mobile() : existingEntry.mobileE164();

                Instant cooloffPeriod = existingEntry == null ? issuedAt.plus(otpProperties.getCooloffPeriod()) : existingEntry.cooloffPeriod();

                Instant configuredExpiresAt = issuedAt.plus(otpProperties.getValidity());

                Instant expiresAt = configuredExpiresAt.isBefore(cooloffPeriod) ? configuredExpiresAt : cooloffPeriod;

                Instant configuredResendAvailableAt = issuedAt.plus(otpProperties.getResendDelay());

                Instant resendAvailableAt = configuredResendAvailableAt.isBefore(cooloffPeriod) ? configuredResendAvailableAt : cooloffPeriod;

                List<String> otpDigests = existingEntry == null ? new ArrayList<>() : new ArrayList<>(existingEntry.otpDigest());

                int otpLength = getOtpLength(otpContext);

                String otp;
                String otpDigest;

                if (otpDigests.size() >= otpProperties.getMaxFailedAttempts()) {
                        throw new ApplicationException(
                                        ErrorCode.OTP_MAX_RETRY_EXHAUSTED,
                                        "Maximum OTP send attempts reached");
                }

                do {
                        otp = randomGenerator.OtpGenerator(otpLength);
                        otpDigest = otpHmacService.generateDigest(otpContext, isd, mobile, otp);
                } while (otpDigests.contains(otpDigest));

                otpDigests.add(otpDigest);
                int verificationAttempts = existingEntry == null ? 0 : existingEntry.maxVerificationAttempt();
                OtpChallengeCacheEntry newEntry = new OtpChallengeCacheEntry(
                                isd,
                                mobile,
                                otpContext,
                                List.copyOf(otpDigests),
                                issuedAt,
                                expiresAt,
                                resendAvailableAt,
                                cooloffPeriod,
                                verificationAttempts);

                Duration remainingTimeToLive = Duration.between(Instant.now(), cooloffPeriod);

                if (remainingTimeToLive.isZero() || remainingTimeToLive.isNegative()) {
                        throw new ApplicationException(ErrorCode.OTP_CHALLENGE_INVALID,
                                        "OTP challenge window has expired");
                }

                AtomicResult result;

                if (existingChallenge == null) {
                        result = otpAtomicStore.createIfAbsent(redisKey, newEntry, remainingTimeToLive);
                } else {
                        result = otpAtomicStore.replaceIfUnchanged(redisKey, existingChallenge.comparisonToken(), newEntry, remainingTimeToLive);
                }

                if (result != AtomicResult.APPLIED) {
                        throw new ApplicationException(ErrorCode.RESOURCE_CONFLICT, "Another OTP request was processed concurrently");
                }

                log.info("OTP generated context={} sendCount={}", otpContext, otpDigests.size());
                log.debug("LOCAL TEST ONLY: generated OTP={}", otp);
                return new OtpChallengeResponse(expiresAt, resendAvailableAt);
        }

        private void validateChallengeBeforeSend(OtpChallengeCacheEntry cacheEntry) {
                Instant now = Instant.now();
                int sendCount = cacheEntry.otpDigest().size();

                if (sendCount >= otpProperties.getMaxFailedAttempts()) {
                        throw new ApplicationException(ErrorCode.OTP_MAX_RETRY_EXHAUSTED, createRetryMessage(cacheEntry.cooloffPeriod()));
                }

                if (now.isBefore(cacheEntry.resendAvailableAt())) {
                        throw new ApplicationException(ErrorCode.OTP_RESEND_TOO_EARLY, createRetryMessage(cacheEntry.resendAvailableAt()));
                }
        }

        private int getOtpLength(OtpContext otpContext) {
                return switch (otpContext) {
                        case CUSTOMER_AUTH -> otpProperties.getCodeLengths().getCustomerAuth();
                        case ADMIN_LOGIN -> otpProperties.getCodeLengths().getAdminLogin();
                        case OPS_LOGIN -> otpProperties.getCodeLengths().getOpsLogin();
                };
        }

        private OtpContext resolveOtpContext(String suppliedContext) {

                if (suppliedContext == null || suppliedContext.isBlank()) {
                        return OtpContext.CUSTOMER_AUTH;
                }

                try {
                        return OtpContext.valueOf(suppliedContext.trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException exception) {
                        throw new ApplicationException(ErrorCode.OTP_CHALLENGE_INVALID, "Invalid OTP context");
                }
        }

        private String buildRedisKey(String isd, String mobile) {
                String mobileHmac = otpHmacService.generateMobileHmac(isd, mobile);
                return redisKeyBuilder.build("OTP", "mobileHmac", mobileHmac);
        }

        private void validateIdentity(OtpRequestModel request) {
                if (request == null) {
                        throw new ApplicationException(ErrorCode.OTP_CHALLENGE_INVALID, "OTP request is required");
                }

                if (request.isd() == null || request.isd().isBlank() || request.mobile() == null || request.mobile().isBlank()) {
                        throw new ApplicationException(ErrorCode.OTP_CHALLENGE_INVALID, "ISD code and mobile number are required");
                }
        }

        private String createRetryMessage(Instant retryAt) {

                Duration remaining = Duration.between(Instant.now(), retryAt);
                if (remaining.isNegative()) {
                        remaining = Duration.ZERO;
                }

                return "Please retry after %s".formatted(remaining);
        }

        private void consumeIncorrectOtp(String redisKey, VersionedOtpChallenge versionedChallenge) {
                OtpChallengeCacheEntry cacheEntry = versionedChallenge.cacheEntry();
                int newAttemptCount = cacheEntry.maxVerificationAttempt() + 1;

                OtpChallengeCacheEntry updatedEntry = new OtpChallengeCacheEntry(
                                cacheEntry.isd(),
                                cacheEntry.mobileE164(),
                                cacheEntry.otpContext(),
                                cacheEntry.otpDigest(),
                                cacheEntry.issuedAt(),
                                cacheEntry.expiresAt(),
                                cacheEntry.resendAvailableAt(),
                                cacheEntry.cooloffPeriod(),
                                newAttemptCount);

                AtomicResult updateResult = otpAtomicStore.replaceIfUnchanged(redisKey, versionedChallenge.comparisonToken(), updatedEntry, Duration.between(Instant.now(), cacheEntry.cooloffPeriod()));

                if (updateResult != AtomicResult.APPLIED) {
                        throw new ApplicationException(ErrorCode.OTP_CHALLENGE_INVALID, "OTP challenge changed or was already consumed");
                }
        }

}