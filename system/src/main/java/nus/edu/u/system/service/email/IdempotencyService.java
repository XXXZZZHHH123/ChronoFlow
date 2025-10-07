package nus.edu.u.system.service.email;

import java.time.Duration;

public interface IdempotencyService {
    boolean tryClaim(String key, Duration ttl);
}
