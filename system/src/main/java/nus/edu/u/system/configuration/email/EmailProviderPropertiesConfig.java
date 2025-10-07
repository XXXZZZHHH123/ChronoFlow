package nus.edu.u.system.configuration.email;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "email")
@Data
@Component
public class EmailProviderPropertiesConfig {
    private String from;
}