package nus.edu.u.system.service.notification;

import java.time.Duration;

public interface RateLimiter {
    boolean allow(String key, int limit, Duration window);
}
