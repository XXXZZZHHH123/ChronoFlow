package nus.edu.u.framework.notification.email;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "notification.email")
public class EmailLimitPropertiesConfig {
    private int rateLimit;
    private Duration rateWindow;
    private Duration idempotencyTtl;
    private String rateKey;
}
