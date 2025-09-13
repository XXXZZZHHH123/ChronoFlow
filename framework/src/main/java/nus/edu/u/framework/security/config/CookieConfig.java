package nus.edu.u.framework.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Lu Shuwen
 * @date 2025-09-05
 */
@Configuration
@ConfigurationProperties(prefix = "chronoflow.cookie")
@Data
public class CookieConfig {

  private boolean httpOnly = true;

  private boolean security = true;
}
