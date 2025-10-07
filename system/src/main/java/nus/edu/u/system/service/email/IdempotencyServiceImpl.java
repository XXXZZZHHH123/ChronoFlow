package nus.edu.u.system.service.email;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

    private final StringRedisTemplate redis;

    @Override
    public boolean tryClaim(String key, Duration ttl) {
        Boolean ok = redis.opsForValue().setIfAbsent(key, "1", ttl);
        return Boolean.TRUE.equals(ok);
    }
}