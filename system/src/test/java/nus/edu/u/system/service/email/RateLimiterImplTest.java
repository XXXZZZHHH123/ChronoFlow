package nus.edu.u.system.service.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RateLimiterImplTest {

    private FakeRedis redis;
    private RateLimiterImpl rateLimiter;

    @BeforeEach
    void setUp() {
        redis = new FakeRedis();
        rateLimiter = new RateLimiterImpl(redis);
    }

    @Test
    void allow_firstRequestSetsExpiryAndReturnsTrue() {
        redis.incrementResult = 1L;

        boolean allowed = rateLimiter.allow("email", 5, Duration.ofSeconds(30));

        assertThat(allowed).isTrue();
        assertThat(redis.expiredKey).isEqualTo("email");
        assertThat(redis.expiredDuration).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void allow_whenCountExceedsLimitReturnsFalse() {
        redis.incrementResult = 6L;

        boolean allowed = rateLimiter.allow("email", 5, Duration.ofSeconds(30));

        assertThat(allowed).isFalse();
        assertThat(redis.expiredKey).isNull();
    }

    @Test
    void allow_whenRedisReturnsNullIsFalse() {
        redis.incrementResult = null;
        boolean allowed = rateLimiter.allow("email", 5, Duration.ofSeconds(30));
        assertThat(allowed).isFalse();
    }

    private static final class FakeRedis extends StringRedisTemplate {
        Long incrementResult = 1L;
        String expiredKey;
        Duration expiredDuration;

        private final ValueOperations<String, String> valueOperations = new FakeValueOperations();

        @Override
        public ValueOperations<String, String> opsForValue() {
            return valueOperations;
        }

        @Override
        public Boolean expire(String key, Duration timeout) {
            this.expiredKey = key;
            this.expiredDuration = timeout;
            return true;
        }

        private final class FakeValueOperations implements ValueOperations<String, String> {
            @Override
            public Long increment(String key) {
                return incrementResult;
            }

            @Override
            public void set(String key, String value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void set(String key, String value, long timeout, TimeUnit unit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void set(String key, String value, Duration timeout) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Boolean setIfAbsent(String key, String value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Boolean setIfAbsent(String key, String value, Duration timeout) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Boolean setIfPresent(String key, String value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Boolean setIfPresent(String key, String value, long timeout, TimeUnit unit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Boolean setIfPresent(String key, String value, Duration timeout) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void multiSet(Map<? extends String, ? extends String> map) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Boolean multiSetIfAbsent(Map<? extends String, ? extends String> map) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String get(Object key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getAndDelete(String key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getAndExpire(String key, long timeout, TimeUnit unit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getAndExpire(String key, Duration timeout) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getAndPersist(String key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getAndSet(String key, String value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<String> multiGet(Collection<String> keys) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Long increment(String key, long delta) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Double increment(String key, double delta) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Long decrement(String key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Long decrement(String key, long delta) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Integer append(String key, String value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String get(String key, long start, long end) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void set(String key, String value, long offset) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Long size(String key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Boolean setBit(String key, long offset, boolean value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Boolean getBit(String key, long offset) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<Long> bitField(String key, BitFieldSubCommands subCommands) {
                throw new UnsupportedOperationException();
            }

            @Override
            public RedisOperations<String, String> getOperations() {
                throw new UnsupportedOperationException();
            }
        }
    }
}
