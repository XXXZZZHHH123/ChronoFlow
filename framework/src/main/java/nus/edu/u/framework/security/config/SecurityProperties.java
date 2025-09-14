package nus.edu.u.framework.security.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Lu Shuwen
 * @date 2025-08-29
 */
@Component
@ConfigurationProperties(prefix = "chronoflow.security")
@Data
public class SecurityProperties {

  @NotNull(message = "Refresh token expire can't be empty")
  private long refreshTokenExpire;
}
