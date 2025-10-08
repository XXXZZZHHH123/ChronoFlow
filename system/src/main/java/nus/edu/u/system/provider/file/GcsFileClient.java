package nus.edu.u.system.provider.file;

import com.google.cloud.storage.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.framework.file.GcsPropertiesConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class GcsFileClient implements FileClient {

    private final Storage storage;
    private final GcsPropertiesConfig gcsConfig;

    @Override
    public FileUploadResult uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File cannot be null or empty");

        try {
            String original = file.getOriginalFilename();
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            String objectName = UUID.randomUUID() + "-" + (original != null ? original : "file");

            BlobInfo info = BlobInfo.newBuilder(BlobId.of(gcsConfig.getBucket(), objectName))
                    .setContentType(contentType)
                    .build();

            storage.createFrom(info, file.getInputStream());

            log.info("Uploaded '{}' to bucket '{}'", objectName, gcsConfig.getBucket());

            return new FileUploadResult(objectName, contentType, file.getSize(), null);
        } catch (IOException e) {
            log.error("Upload failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file to GCS", e);
        }
    }

    /** Generate a fresh signed URL for an existing object. */
    public String generateSignedUrl(String objectName) {
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(gcsConfig.getBucket(), objectName)).build();
        URL signedUrl = storage.signUrl(
                blobInfo,
                gcsConfig.getSignedUrlExpiryMinutes(),
                TimeUnit.MINUTES,
                Storage.SignUrlOption.withV4Signature()
        );
        return signedUrl.toString();
    }
}