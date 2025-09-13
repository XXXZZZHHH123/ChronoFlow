package nus.edu.u.framework.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * @author Lu Shuwen
 * @date 2025-09-09
 */
@Configuration
public class CorsConfig {

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowCredentials(true); // Allow Cookie
    config.addAllowedOriginPattern("*"); // Allow all sources
    config.addAllowedHeader("*"); // Allow all request headers
    config.addAllowedMethod("*"); // Allow all request methods (GET, POST, PUT, DELETE...)

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
