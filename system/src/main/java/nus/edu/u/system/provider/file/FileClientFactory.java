package nus.edu.u.system.provider.file;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileClientFactory {

    private final GcsFileClient gcsFileClient;

    public FileClient create(String provider) {
        if (provider == null || provider.isBlank())
            throw new IllegalArgumentException("File provider cannot be null or blank");

        return switch (provider.trim().toLowerCase()) {
            case "gcs" -> gcsFileClient;
            default -> throw new IllegalArgumentException("Unsupported file provider: " + provider);
        };
    }
}