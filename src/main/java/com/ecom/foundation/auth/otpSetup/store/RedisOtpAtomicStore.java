package com.ecom.foundation.auth.otpSetup.store;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;

import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import com.ecom.foundation.auth.otpSetup.dto.OtpChallengeCacheEntry;
import com.ecom.foundation.common.redis.RedisValidation;
import com.ecom.foundation.common.redis.RedisValueStoreException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public final class RedisOtpAtomicStore implements OtpAtomicStore {

    private static final long RESULT_APPLIED = 1L;
    private static final long RESULT_CONFLICT = 0L;
    private static final long RESULT_NOT_FOUND = -1L;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final DefaultRedisScript<Long> replaceIfUnchangedScript;
    private final DefaultRedisScript<Long> deleteIfUnchangedScript;

    public RedisOtpAtomicStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;

        this.replaceIfUnchangedScript = new DefaultRedisScript<>();
        this.replaceIfUnchangedScript.setLocation(new ClassPathResource("redis/otp/replace-if-unchanged.lua"));
        this.replaceIfUnchangedScript.setResultType(Long.class);

        this.deleteIfUnchangedScript = new DefaultRedisScript<>();
        this.deleteIfUnchangedScript.setLocation(new ClassPathResource("redis/otp/delete-if-unchanged.lua"));
        this.deleteIfUnchangedScript.setResultType(Long.class);
    }

    @Override
    public Optional<VersionedOtpChallenge> find(String redisKey) {
        RedisValidation.validateKey(redisKey);

        try {
            String storedJson = redisTemplate.opsForValue().get(redisKey);

            if (storedJson == null) {
                return Optional.empty();
            }

            OtpChallengeCacheEntry cacheEntry = objectMapper.readValue(storedJson, OtpChallengeCacheEntry.class);

            return Optional.of(new VersionedOtpChallenge(cacheEntry, storedJson));

        } catch (JsonProcessingException | DataAccessException exception) {
            throw new RedisValueStoreException( "Could not read OTP challenge: " + redisKey, exception);
        }
    }

    @Override
    public AtomicResult createIfAbsent(String redisKey, OtpChallengeCacheEntry cacheEntry, Duration timeToLive) {
        RedisValidation.validateKey(redisKey);
        RedisValidation.validateValue(cacheEntry);
        RedisValidation.validateTtl(timeToLive);

        try {
            String json = objectMapper.writeValueAsString(cacheEntry);

            Boolean created = redisTemplate.opsForValue().setIfAbsent(redisKey, json, timeToLive);

            return Boolean.TRUE.equals(created) ? AtomicResult.APPLIED : AtomicResult.CONFLICT;

        } catch (JsonProcessingException | DataAccessException exception) {
            throw new RedisValueStoreException("Could not create OTP challenge: " + redisKey, exception);
        }
    }

    @Override
    public AtomicResult replaceIfUnchanged(String redisKey, String comparisonToken, OtpChallengeCacheEntry updatedEntry, Duration timeToLive) {
        RedisValidation.validateKey(redisKey);
        RedisValidation.validateValue(comparisonToken);
        RedisValidation.validateValue(updatedEntry);
        RedisValidation.validateTtl(timeToLive);

        long timeToLiveMilliseconds = timeToLive.toMillis();

        if (timeToLiveMilliseconds <= 0) {
            throw new IllegalArgumentException("Time To Live must be greater than zero milliseconds");
        }

        try {
            String updatedJson = objectMapper.writeValueAsString(updatedEntry);

            Long result = redisTemplate.execute(replaceIfUnchangedScript, Collections.singletonList(redisKey), comparisonToken, updatedJson, Long.toString(timeToLiveMilliseconds));

            return mapResult(result);
        } catch (JsonProcessingException | DataAccessException exception) {
            throw new RedisValueStoreException("Could not replace OTP challenge: " + redisKey,exception);
        }
    }

    @Override
    public AtomicResult deleteIfUnchanged(String redisKey, String comparisonToken) {
        RedisValidation.validateKey(redisKey);
        RedisValidation.validateValue(comparisonToken);

        try {
            Long result = redisTemplate.execute(deleteIfUnchangedScript, Collections.singletonList(redisKey), comparisonToken);

            return mapResult(result);
        } catch (DataAccessException exception) {
            throw new RedisValueStoreException("Could not delete OTP challenge: " + redisKey, exception);
        }
    }

    private AtomicResult mapResult(Long result) {
        if (result == null) {
            throw new RedisValueStoreException(
                    "Redis returned no atomic operation result",
                    null
            );
        }

        if (result == RESULT_APPLIED) {
            return AtomicResult.APPLIED;
        }

        if (result == RESULT_NOT_FOUND) {
            return AtomicResult.NOT_FOUND;
        }

        if (result == RESULT_CONFLICT) {
            return AtomicResult.CONFLICT;
        }

        throw new RedisValueStoreException("Unknown Redis atomic operation result: " + result, null);
    }
}