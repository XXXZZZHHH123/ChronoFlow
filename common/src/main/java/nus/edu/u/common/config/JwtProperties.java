package nus.edu.u.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Jwt configuration class
 *
 * @author Lu Shuwen
 * @date 2025-08-28
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;

    private Long accessExpire;

    private Long refreshExpire;
}
