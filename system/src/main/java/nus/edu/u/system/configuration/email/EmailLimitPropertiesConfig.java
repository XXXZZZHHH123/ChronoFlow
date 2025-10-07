package nus.edu.u.system.configuration.email;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "notification.email")
public class EmailLimitPropertiesConfig {
    private int rateLimit;
    private Duration rateWindow;
    private Duration idempotencyTtl;
    private String rateKey;
}