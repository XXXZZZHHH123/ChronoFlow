package nus.edu.u.system.service.notification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RateLimiterImplTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private RateLimiterImpl sut;

    @BeforeEach
    void setUp() {
        redis = Mockito.mock(StringRedisTemplate.class);
        valueOps = Mockito.mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        sut = new RateLimiterImpl(redis);
    }

    @Test
    void allow_firstHit_setsTtl_andAllows() {
        String key = "rate:user:1";
        Duration window = Duration.ofSeconds(60);
        int limit = 5;

        when(valueOps.increment(key)).thenReturn(1L);

        boolean allowed = sut.allow(key, limit, window);

        assertTrue(allowed);
        verify(redis).expire(key, window);
    }

    @Test
    void allow_withinLimit_noTtlReset() {
        String key = "rate:user:2";
        Duration window = Duration.ofSeconds(30);
        int limit = 5;

        when(valueOps.increment(key)).thenReturn(3L); // not first hit

        boolean allowed = sut.allow(key, limit, window);

        assertTrue(allowed);
        verify(redis, never()).expire(eq(key), any(Duration.class));
    }

    @Test
    void block_whenOverLimit() {
        String key = "rate:user:3";
        Duration window = Duration.ofSeconds(45);
        int limit = 5;

        when(valueOps.increment(key)).thenReturn(6L); // over limit

        boolean allowed = sut.allow(key, limit, window);

        assertFalse(allowed);
        verify(redis, never()).expire(eq(key), any(Duration.class));
    }

    @Test
    void returnsFalse_whenIncrementReturnsNull() {
        String key = "rate:user:4";
        Duration window = Duration.ofSeconds(10);
        int limit = 1;

        when(valueOps.increment(key)).thenReturn(null);

        boolean allowed = sut.allow(key, limit, window);

        assertFalse(allowed);
        verify(redis, never()).expire(eq(key), any(Duration.class));
    }

    @Test
    void firstHit_butLimitZero_setsTtl_andBlocks() {
        String key = "rate:user:5";
        Duration window = Duration.ofSeconds(20);
        int limit = 0;

        when(valueOps.increment(key)).thenReturn(1L);

        boolean allowed = sut.allow(key, limit, window);

        assertFalse(allowed);
        verify(redis).expire(key, window);
    }
}