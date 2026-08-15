package com.ecom.foundation.common.redis;

import java.util.Optional;

import ch.qos.logback.core.util.Duration;

public interface RedisValuestore {
    <T> void save( String key, T value, Duration ttl );

    <T> Optional<T> find( String key, Class<T> valueType );

    boolean delete(String key);

    boolean exists(String key);
}
