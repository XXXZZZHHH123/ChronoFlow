package nus.edu.u.system.service.notification;

import java.time.Duration;

public interface IdempotencyService {
    boolean tryClaim(String key, Duration ttl);
}
