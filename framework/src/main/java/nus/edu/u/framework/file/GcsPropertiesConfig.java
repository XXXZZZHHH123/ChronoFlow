package nus.edu.u.framework.file;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "gcs")
public class GcsPropertiesConfig {

    @NotBlank
    private String bucket;

    @Min(1)
    private long signedUrlExpiryMinutes;
}
