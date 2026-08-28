package com.ecom.foundation.common.redis;

import static com.ecom.foundation.common.redis.RedisValidation.validateKey;
import static com.ecom.foundation.common.redis.RedisValidation.validateTtl;
import static com.ecom.foundation.common.redis.RedisValidation.validateValue;


import java.time.Duration;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class RedisValueStoreImpl implements RedisValuestore {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisValueStoreImpl(
        StringRedisTemplate redisTemplate,
        ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> void save(String key, T value, Duration ttl) {
        validateKey(key);
        validateValue(value);
        validateTtl(ttl);

        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException exception) {
            throw new RedisValueStoreException("Could not serialize Redis value for key " + key, exception);
        } catch (DataAccessException exception) {
            throw new RedisValueStoreException("Could not save value to redis for key " + key, exception);
        }
    }

    @Override
    public <T> Optional<T> find (String key, Class<T> valueType) {
        validateKey(key);
        try {

            String json = redisTemplate.opsForValue().get(key);
            if(json == null){
                return Optional.empty();
            }
            T value = objectMapper.readValue(json, valueType);
            return Optional.of(value);
        } catch (JsonProcessingException exception) {
            throw new RedisValueStoreException(key, exception);
        }
    }

    @Override
    public boolean delete(String key) {
        validateKey(key);
        try {
            boolean status = redisTemplate.delete(key);
            return Boolean.TRUE.equals(status);
        } catch (DataAccessException exception) {
            throw new RedisValueStoreException(key, exception);
        }
    }

    @Override
    public boolean exists(String key) {
        validateKey(key);
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (DataAccessException exception) {
            throw new RedisValueStoreException(key, exception);
        }
    }

}
