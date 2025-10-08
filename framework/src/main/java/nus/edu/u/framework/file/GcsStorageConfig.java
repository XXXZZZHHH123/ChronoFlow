package nus.edu.u.framework.file;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GcsStorageConfig {

    public static final String GCP_SERVICE_ENV_NAME = "GCP_SERVICE_ACCOUNT_JSON";

    @Bean
    public Storage storage() throws IOException {
        String encoded = System.getenv(GCP_SERVICE_ENV_NAME);
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalStateException(GCP_SERVICE_ENV_NAME + " is not set");
        }

        byte[] decodedBytes = Base64.getDecoder().decode(encoded);
        try (var inputStream = new ByteArrayInputStream(decodedBytes)) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(inputStream);
            return StorageOptions.newBuilder()
                    .setCredentials(credentials)
                    .build()
                    .getService();
        }
    }
}
