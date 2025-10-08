package nus.edu.u.framework.file;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class GcsStorageConfig {

    public static final String GCP_SERVICE_ENV_NAME = "GCP_SERVICE_ACCOUNT_JSON";

    @Bean
    public Storage storage() throws IOException {
        String json = System.getenv(GCP_SERVICE_ENV_NAME);
        GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))
        );

        return StorageOptions.newBuilder()
                .setCredentials(credentials)
                .build()
                .getService();
    }
}