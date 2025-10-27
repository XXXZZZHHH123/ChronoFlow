package nus.edu.u.system.provider.file;

import static org.assertj.core.api.Assertions.*;

import com.google.cloud.storage.Storage;
import java.lang.reflect.Proxy;
import nus.edu.u.framework.file.GcsPropertiesConfig;
import org.junit.jupiter.api.Test;

class FileClientFactoryTest {

    @Test
    void create_returnsGcsClient() {
        FileClientFactory factory = new FileClientFactory(newGcsClient());

        assertThat(factory.create(" gcs ")).isInstanceOf(GcsFileClient.class);
    }

    @Test
    void create_withNullProvider_throws() {
        FileClientFactory factory = new FileClientFactory(newGcsClient());
        assertThatThrownBy(() -> factory.create(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    void create_withUnsupportedProvider_throws() {
        FileClientFactory factory = new FileClientFactory(newGcsClient());
        assertThatThrownBy(() -> factory.create("s3"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported file provider");
    }

    private static GcsFileClient newGcsClient() {
        Storage storage =
                (Storage)
                        Proxy.newProxyInstance(
                                Storage.class.getClassLoader(),
                                new Class<?>[] {Storage.class},
                                (proxy, method, args) -> {
                                    switch (method.getName()) {
                                        case "createFrom":
                                            return null;
                                        case "signUrl":
                                            try {
                                                return new java.net.URL("https://example.com");
                                            } catch (java.net.MalformedURLException e) {
                                                throw new RuntimeException(e);
                                            }
                                        case "delete":
                                            return true;
                                        case "getOptions":
                                        case "close":
                                            return null;
                                        default:
                                            throw new UnsupportedOperationException(
                                                    method.getName());
                                    }
                                });
        GcsPropertiesConfig config = new GcsPropertiesConfig();
        config.setBucket("bucket");
        config.setSignedUrlExpiryMinutes(5);
        return new GcsFileClient(storage, config);
    }
}
