package nus.edu.u.framework.notification.email;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "email")
@Data
@Component
public class EmailProviderPropertiesConfig {
    private String from;

    private static volatile EmailProviderPropertiesConfig INSTANCE;

    @PostConstruct
    void init() { INSTANCE = this; }

    public static EmailProviderPropertiesConfig current() {
        EmailProviderPropertiesConfig cfg = INSTANCE;
        if (cfg == null) {
            throw new IllegalStateException(
                    "EmailProviderPropertiesConfig not initialized. Is Spring context up?"
            );
        }
        return cfg;
    }
}