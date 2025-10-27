package nus.edu.u.system.service.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class IdempotencyServiceImplTest {

    private FakeRedis redis;
    private IdempotencyServiceImpl service;

    @BeforeEach
    void setUp() {
        redis = new FakeRedis();
        service = new IdempotencyServiceImpl(redis);
    }

    @Test
    void tryClaim_returnsTrueWhenRedisAccepts() {
        redis.setIfAbsentResult = Boolean.TRUE;

        boolean claimed = service.tryClaim("email:key", Duration.ofSeconds(10));

        assertThat(claimed).isTrue();
        assertThat(redis.lastKey).isEqualTo("email:key");
        assertThat(redis.lastTtl).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void tryClaim_returnsFalseWhenKeyAlreadyExists() {
        redis.setIfAbsentResult = Boolean.FALSE;

        boolean claimed = service.tryClaim("email:key", Duration.ofSeconds(5));

        assertThat(claimed).isFalse();
    }

    @Test
    void tryClaim_nullResultIsFalse() {
        redis.setIfAbsentResult = null;

        assertThat(service.tryClaim("email:key", Duration.ofSeconds(5))).isFalse();
    }

    private static final class FakeRedis extends StringRedisTemplate {
        Boolean setIfAbsentResult = Boolean.TRUE;
        String lastKey;
        Duration lastTtl;

        @Override
        public ValueOperations<String, String> opsForValue() {
            return (ValueOperations<String, String>)
                    Proxy.newProxyInstance(
                            ValueOperations.class.getClassLoader(),
                            new Class<?>[] {ValueOperations.class},
                            (proxy, method, args) -> {
                                if ("setIfAbsent".equals(method.getName())) {
                                    if (args.length == 3 && args[2] instanceof Duration duration) {
                                        lastKey = (String) args[0];
                                        lastTtl = duration;
                                        return setIfAbsentResult;
                                    }
                                    if (args.length == 2) {
                                        lastKey = (String) args[0];
                                        return setIfAbsentResult;
                                    }
                                }
                                throw new UnsupportedOperationException(method.getName());
                            });
        }
    }
}
