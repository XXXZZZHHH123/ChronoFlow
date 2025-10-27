package nus.edu.u.system.provider.file;

import static org.assertj.core.api.Assertions.*;

import com.google.cloud.storage.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import nus.edu.u.framework.file.GcsPropertiesConfig;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

class GcsFileClientTest {

    @Test
    void uploadFile_persistsObjectAndReturnsMetadata() throws IOException {
        RecordingStorage recording = new RecordingStorage();
        GcsFileClient client = new GcsFileClient(recording.asStorage(), defaultConfig());

        MultipartFile file =
                new InMemoryMultipartFile(
                        "report.pdf",
                        "application/pdf",
                        "Hello GCS".getBytes(StandardCharsets.UTF_8));

        FileClient.FileUploadResult result = client.uploadFile(file);

        assertThat(result.objectName()).isEqualTo(recording.lastUploaded.getBlobId().getName());
        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(recording.uploadedBytes)
                .containsExactly("Hello GCS".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void uploadFile_whenEmpty_throwsIllegalArgumentException() {
        RecordingStorage recording = new RecordingStorage();
        GcsFileClient client = new GcsFileClient(recording.asStorage(), defaultConfig());

        MultipartFile empty =
                new InMemoryMultipartFile("empty.txt", "text/plain", new byte[0], true);

        assertThatThrownBy(() -> client.uploadFile(empty))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File cannot be null or empty");
    }

    @Test
    void uploadFile_whenStorageThrows_wrapsRuntimeException() {
        RecordingStorage recording = new RecordingStorage();
        recording.throwOnCreate = true;
        GcsFileClient client = new GcsFileClient(recording.asStorage(), defaultConfig());

        MultipartFile file =
                new InMemoryMultipartFile(
                        "error.txt", "text/plain", "data".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> client.uploadFile(file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to upload file to GCS");
    }

    @Test
    void generateSignedUrl_usesStorage() {
        RecordingStorage recording = new RecordingStorage();
        recording.signedUrlToReturn = url("https://example.com/object");
        GcsFileClient client = new GcsFileClient(recording.asStorage(), defaultConfig());

        String generated = client.generateSignedUrl("obj");

        assertThat(generated).isEqualTo("https://example.com/object");
        assertThat(recording.lastSigned).isEqualTo("obj");
    }

    @Test
    void deleteQuietly_swallowsExceptions() {
        RecordingStorage recording = new RecordingStorage();
        recording.throwOnDelete = true;
        GcsFileClient client = new GcsFileClient(recording.asStorage(), defaultConfig());

        assertThatCode(() -> client.deleteQuietly("temp")).doesNotThrowAnyException();
    }

    @Test
    void deleteQuietly_successfulDeleteCallsStorage() {
        RecordingStorage recording = new RecordingStorage();
        GcsFileClient client = new GcsFileClient(recording.asStorage(), defaultConfig());

        client.deleteQuietly("object-key");

        assertThat(recording.lastDeleted.getName()).isEqualTo("object-key");
    }

    private static GcsPropertiesConfig defaultConfig() {
        GcsPropertiesConfig config = new GcsPropertiesConfig();
        config.setBucket("bucket");
        config.setSignedUrlExpiryMinutes(5);
        return config;
    }

    private static URL url(String value) {
        try {
            return new URL(value);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class RecordingStorage {
        BlobInfo lastUploaded;
        byte[] uploadedBytes;
        BlobId lastDeleted;
        URL signedUrlToReturn = url("https://example.com/default");
        String lastSigned;
        boolean throwOnDelete;
        boolean throwOnCreate;

        Storage asStorage() {
            return (Storage)
                    Proxy.newProxyInstance(
                            Storage.class.getClassLoader(),
                            new Class<?>[] {Storage.class},
                            (proxy, method, args) -> {
                                switch (method.getName()) {
                                    case "createFrom" -> {
                                        lastUploaded = (BlobInfo) args[0];
                                        InputStream in = (InputStream) args[1];
                                        if (throwOnCreate) {
                                            throw new IOException("simulated failure");
                                        }
                                        uploadedBytes = in.readAllBytes();
                                        return null;
                                    }
                                    case "signUrl" -> {
                                        BlobInfo info = (BlobInfo) args[0];
                                        lastSigned = info.getBlobId().getName();
                                        return signedUrlToReturn;
                                    }
                                    case "delete" -> {
                                        if (throwOnDelete) {
                                            throw new RuntimeException("delete failure");
                                        }
                                        lastDeleted = (BlobId) args[0];
                                        return true;
                                    }
                                    case "getOptions" -> {
                                        return null;
                                    }
                                    case "close" -> {
                                        return null;
                                    }
                                    default ->
                                            throw new UnsupportedOperationException(
                                                    "Method not supported: " + method.getName());
                                }
                            });
        }
    }

    private static final class InMemoryMultipartFile implements MultipartFile {
        private final String originalFilename;
        private final String contentType;
        private final byte[] bytes;
        private final boolean emptyOverride;

        private InMemoryMultipartFile(String originalFilename, String contentType, byte[] bytes) {
            this(originalFilename, contentType, bytes, false);
        }

        private InMemoryMultipartFile(
                String originalFilename, String contentType, byte[] bytes, boolean emptyOverride) {
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.bytes = bytes;
            this.emptyOverride = emptyOverride;
        }

        @Override
        public String getName() {
            return originalFilename;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return emptyOverride || bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(java.io.File dest) {
            throw new UnsupportedOperationException();
        }
    }
}
