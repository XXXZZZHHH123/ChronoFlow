package nus.edu.u.system.service.email;

import java.time.Duration;

public interface RateLimiter {
    boolean allow(String key, int limit, Duration window);
}