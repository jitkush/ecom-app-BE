package com.ecom.foundation.auth.otpSetup.store;

import java.time.Duration;
import java.util.Optional;

import com.ecom.foundation.auth.otpSetup.dto.OtpChallengeCacheEntry;

public interface OtpAtomicStore {

    Optional<VersionedOtpChallenge> find(String redisKey);

    AtomicResult createIfAbsent(
            String redisKey,
            OtpChallengeCacheEntry cacheEntry,
            Duration timeToLive
    );

    AtomicResult replaceIfUnchanged(
            String redisKey,
            String comparisonToken,
            OtpChallengeCacheEntry updatedEntry,
            Duration timeToLive
    );

    AtomicResult deleteIfUnchanged(
            String redisKey,
            String comparisonToken
    );

    enum AtomicResult {
        APPLIED,
        NOT_FOUND,
        CONFLICT
    }

    record VersionedOtpChallenge(
            OtpChallengeCacheEntry cacheEntry,
            String comparisonToken
    ) {
    }
}