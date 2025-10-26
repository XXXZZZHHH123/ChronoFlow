package nus.edu.u.system.service.file;

import static org.assertj.core.api.Assertions.*;

import com.google.cloud.storage.Storage;
import java.lang.reflect.Proxy;
import java.util.*;
import nus.edu.u.framework.file.FileProviderPropertiesConfig;
import nus.edu.u.framework.file.GcsPropertiesConfig;
import nus.edu.u.system.domain.dataobject.file.FileDO;
import nus.edu.u.system.domain.vo.file.FileResultVO;
import nus.edu.u.system.mapper.file.FileMapper;
import nus.edu.u.system.provider.file.FileClient;
import nus.edu.u.system.provider.file.FileClientFactory;
import nus.edu.u.system.provider.file.GcsFileClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

class FileStorageServiceImplTest {

    private InMemoryFileMapper fileMapper;
    private TestFileClientFactory fileClientFactory;
    private FileProviderPropertiesConfig providerConfig;
    private FileStorageServiceImpl fileStorageService;

    @BeforeEach
    void setUp() {
        fileMapper = new InMemoryFileMapper();
        providerConfig = new FileProviderPropertiesConfig();
        providerConfig.setProvider("gcs");
        fileClientFactory = new TestFileClientFactory(new StubGcsFileClient());
        fileStorageService =
                new FileStorageServiceImpl(fileClientFactory, providerConfig, fileMapper);
    }

    @Test
    void downloadFile_returnsSignedUrl() {
        fileMapper.store(
                FileDO.builder()
                        .id(1L)
                        .taskLogId(10L)
                        .provider("gcs")
                        .objectName("obj-1")
                        .name("report.pdf")
                        .type("application/pdf")
                        .size(100L)
                        .build());

        FileResultVO result = fileStorageService.downloadFile(10L);

        assertThat(result.getSignedUrl()).isEqualTo("signed://obj-1");
        assertThat(result.getName()).isEqualTo("report.pdf");
    }

    @Test
    void downloadFile_whenMissing_throws() {
        assertThatThrownBy(() -> fileStorageService.downloadFile(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File not found");
    }

    @Test
    void downloadFilesByTaskLogId_returnsResults() {
        fileMapper.store(
                FileDO.builder()
                        .id(1L)
                        .taskLogId(20L)
                        .provider("gcs")
                        .objectName("obj-1")
                        .name("a.txt")
                        .type("text/plain")
                        .size(10L)
                        .build());
        fileMapper.store(
                FileDO.builder()
                        .id(2L)
                        .taskLogId(20L)
                        .provider("gcs")
                        .objectName("obj-2")
                        .name("b.txt")
                        .type("text/plain")
                        .size(20L)
                        .build());

        List<FileResultVO> files = fileStorageService.downloadFilesByTaskLogId(20L);

        assertThat(files).hasSize(2);
        assertThat(files.get(0).getSignedUrl()).isEqualTo("signed://obj-1");
    }

    @Test
    void downloadFilesByTaskLogId_unsupportedProviderThrows() {
        providerConfig.setProvider("local");
        fileClientFactory.register("local", new NonGcsFileClient());
        fileMapper.store(
                FileDO.builder()
                        .id(3L)
                        .taskLogId(30L)
                        .provider("local")
                        .objectName("obj")
                        .name("c.txt")
                        .type("text/plain")
                        .size(5L)
                        .build());

        assertThatThrownBy(() -> fileStorageService.downloadFilesByTaskLogId(30L))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static final class StubGcsFileClient extends GcsFileClient {
        private StubGcsFileClient() {
            super(
                    (Storage)
                            Proxy.newProxyInstance(
                                    Storage.class.getClassLoader(),
                                    new Class<?>[] {Storage.class},
                                    (proxy, method, args) -> null),
                    config());
        }

        private static GcsPropertiesConfig config() {
            GcsPropertiesConfig config = new GcsPropertiesConfig();
            config.setBucket("bucket");
            config.setSignedUrlExpiryMinutes(5);
            return config;
        }

        @Override
        public FileUploadResult uploadFile(MultipartFile file) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String generateSignedUrl(String objectName) {
            return "signed://" + objectName;
        }

        @Override
        public void deleteQuietly(String objectName) {}
    }

    private static final class NonGcsFileClient implements FileClient {
        @Override
        public FileUploadResult uploadFile(MultipartFile file) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class TestFileClientFactory extends FileClientFactory {
        private final Map<String, FileClient> clients = new HashMap<>();

        private TestFileClientFactory(GcsFileClient gcs) {
            super(gcs);
            clients.put("gcs", gcs);
        }

        void register(String provider, FileClient client) {
            clients.put(provider, client);
        }

        @Override
        public FileClient create(String provider) {
            FileClient client = clients.get(provider.trim().toLowerCase());
            if (client == null) {
                return super.create(provider);
            }
            return client;
        }
    }

    private static final class InMemoryFileMapper implements FileMapper {
        private final Map<Long, FileDO> store = new HashMap<>();
        private long idSeq = 1;

        void store(FileDO file) {
            if (file.getId() == null) {
                file.setId(idSeq++);
            }
            store.put(file.getId(), file);
        }

        @Override
        public FileDO selectOne(
                com.baomidou.mybatisplus.core.conditions.Wrapper<FileDO> queryWrapper) {
            return store.values().stream().findFirst().orElse(null);
        }

        @Override
        public List<FileDO> selectList(
                com.baomidou.mybatisplus.core.conditions.Wrapper<FileDO> queryWrapper) {
            return new ArrayList<>(store.values());
        }

        @Override
        public int insertBatch(List<FileDO> list) {
            list.forEach(this::store);
            return list.size();
        }

        @Override
        public int insert(FileDO entity) {
            store(entity);
            return 1;
        }

        @Override
        public FileDO selectById(java.io.Serializable id) {
            return store.get(id);
        }

        // Remaining BaseMapper methods throw UnsupportedOperationException if invoked
        @Override
        public int deleteById(java.io.Serializable id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteById(FileDO entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int delete(com.baomidou.mybatisplus.core.conditions.Wrapper<FileDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteBatchIds(java.util.Collection<?> idList) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int updateById(FileDO entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int update(
                FileDO entity,
                com.baomidou.mybatisplus.core.conditions.Wrapper<FileDO> updateWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<FileDO> selectBatchIds(
                java.util.Collection<? extends java.io.Serializable> idList) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectBatchIds(
                java.util.Collection<? extends java.io.Serializable> idList,
                org.apache.ibatis.session.ResultHandler<FileDO> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.lang.Long selectCount(
                com.baomidou.mybatisplus.core.conditions.Wrapper<FileDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectList(
                com.baomidou.mybatisplus.core.conditions.Wrapper<FileDO> queryWrapper,
                org.apache.ibatis.session.ResultHandler<FileDO> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<FileDO> selectList(
                com.baomidou.mybatisplus.core.metadata.IPage<FileDO> page,
                com.baomidou.mybatisplus.core.conditions.Wrapper<FileDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectList(
                com.baomidou.mybatisplus.core.metadata.IPage<FileDO> page,
                com.baomidou.mybatisplus.core.conditions.Wrapper<FileDO> queryWrapper,
                org.apache.ibatis.session.ResultHandler<FileDO> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<java.util.Map<String, Object>> selectMaps(
                com.baomidou.mybatisplus.core.conditions.Wrapper<FileDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectMaps(
                com.baomidou.mybatisplus.core.conditions.Wrapper<FileDO> queryWrapper,
                org.apache.ibatis.session.ResultHandler<java.util.Map<String, Object>>
                        resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.List<java.util.Map<String, Object>> selectMaps(
                com.baomidou.mybatisplus.core.metadata.IPage<
                                ? extends java.util.Map<String, Object>>
                        page,
                com.baomidou.mybatisplus.core.conditions.Wrapper<FileDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectMaps(
                com.baomidou.mybatisplus.core.metadata.IPage<
                                ? extends java.util.Map<String, Object>>
                        page,
                com.baomidou.mybatisplus.core.conditions.Wrapper<FileDO> queryWrapper,
                org.apache.ibatis.session.ResultHandler<java.util.Map<String, Object>>
                        resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <E> java.util.List<E> selectObjs(
                com.baomidou.mybatisplus.core.conditions.Wrapper<FileDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <E> void selectObjs(
                com.baomidou.mybatisplus.core.conditions.Wrapper<FileDO> queryWrapper,
                org.apache.ibatis.session.ResultHandler<E> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <P extends com.baomidou.mybatisplus.core.metadata.IPage<FileDO>> P selectPage(
                P page, com.baomidou.mybatisplus.core.conditions.Wrapper<FileDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <
                        P extends
                                com.baomidou.mybatisplus.core.metadata.IPage<
                                                java.util.Map<String, Object>>>
                P selectMapsPage(
                        P page,
                        com.baomidou.mybatisplus.core.conditions.Wrapper<FileDO> queryWrapper) {
            throw new UnsupportedOperationException();
        }
    }
}
