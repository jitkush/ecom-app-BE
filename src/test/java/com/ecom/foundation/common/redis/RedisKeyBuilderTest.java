package com.ecom.foundation.common.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

public class RedisKeyBuilderTest {
    private final RedisKeyProperties property = new RedisKeyProperties("aj", "local");
    private final RedisKeyBuilder builder = new RedisKeyBuilder(property);

    @Test
    void shouldBuilRedisKey() {
        String result = builder.build(
            " AUTH ",
            " OTP ",
            " User-abc "
        );
        System.out.println(result);
        assertThat(result).isEqualTo("aj:local:auth:otp:User-abc");
    }

    @Test 
    void shouldNotBuildKeyWithColon(){
         assertThatThrownBy(() ->
            builder.build(
                    "auth",
                    "otp:attempts",
                    "User-ABC"
            )
    )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("resource");
    }   
}
