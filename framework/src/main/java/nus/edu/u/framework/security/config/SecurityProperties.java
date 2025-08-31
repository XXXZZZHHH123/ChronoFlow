package nus.edu.u.framework.security.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Lu Shuwen
 * @date 2025-08-29
 */
@Component
@ConfigurationProperties(prefix = "chronoflow.security")
@Validated
@Data
public class SecurityProperties {

    @NotEmpty(message = "Token header can't be empty")
    private String tokenHeader = "Authentication";

    @NotEmpty(message = "Token parameter can't be empty")
    private String tokenParameter = "token";

    /**
     * mock mode switch
     */
    @NotNull(message = "mock switch can't be empty")
    private Boolean mockEnable = false;

    /**
     * mock mode secret key
     */
    @NotEmpty(message = "mock secret key can't be empty")
    private String mockSecret = "mock_secret";

    /**
     * Access URL with no authentication
     */
    private List<String> permitAllUrls = Collections.emptyList();

}

