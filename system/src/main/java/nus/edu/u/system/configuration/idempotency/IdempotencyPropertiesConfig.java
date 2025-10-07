package nus.edu.u.system.configuration.idempotency;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "notification.idempotency")
public class IdempotencyPropertiesConfig {
    private String emailPrefix;
    private String pushPrefix;
}