package nus.edu.u.system.service.file;

import static org.assertj.core.api.Assertions.*;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.google.cloud.storage.Storage;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import nus.edu.u.framework.file.FileProviderPropertiesConfig;
import nus.edu.u.framework.file.GcsPropertiesConfig;
import nus.edu.u.system.domain.dataobject.file.FileDO;
import nus.edu.u.system.domain.vo.file.FileResultVO;
import nus.edu.u.system.domain.vo.file.FileUploadReqVO;
import nus.edu.u.system.mapper.file.FileMapper;
import nus.edu.u.system.provider.file.FileClient;
import nus.edu.u.system.provider.file.FileClientFactory;
import nus.edu.u.system.provider.file.GcsFileClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
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
    void uploadToTaskLog_persistsMetadataAndReturnsSignedUrls() {
        InMemoryFileMapper mapper = new InMemoryFileMapper();
        RecordingGcsFileClient client = new RecordingGcsFileClient();
        FileProviderPropertiesConfig config = new FileProviderPropertiesConfig();
        config.setProvider("gcs");
        FileStorageServiceImpl service =
                new FileStorageServiceImpl(new TestFileClientFactory(client), config, mapper);

        MockMultipartFile first =
                new MockMultipartFile("files", "first.txt", "text/plain", "alpha".getBytes());
        MockMultipartFile second =
                new MockMultipartFile("files", "second.txt", "text/plain", "beta".getBytes());
        FileUploadReqVO req = new FileUploadReqVO();
        req.setTaskLogId(11L);
        req.setEventId(22L);
        req.setFiles(List.of(first, second));

        client.respondWith(
                "first.txt",
                new FileClient.FileUploadResult("obj-1", "text/plain", first.getSize(), null));
        client.respondWith(
                "second.txt",
                new FileClient.FileUploadResult("obj-2", "text/plain", second.getSize(), null));

        try (StpLogicRestorer ignored = new StpLogicRestorer(123L)) {
            List<FileResultVO> results = service.uploadToTaskLog(req);

            assertThat(results).hasSize(2);
            assertThat(results)
                    .extracting(FileResultVO::getObjectName)
                    .containsExactly("obj-1", "obj-2");
            assertThat(results)
                    .extracting(FileResultVO::getSignedUrl)
                    .containsExactly("signed://obj-1", "signed://obj-2");
        }

        List<FileDO> stored = mapper.all();
        assertThat(stored).hasSize(2);
        FileDO firstEntity =
                stored.stream()
                        .filter(f -> "obj-1".equals(f.getObjectName()))
                        .findFirst()
                        .orElseThrow();
        assertThat(firstEntity.getTaskLogId()).isEqualTo(11L);
        assertThat(firstEntity.getEventId()).isEqualTo(22L);
        assertThat(firstEntity.getProvider()).isEqualTo("gcs");
        assertThat(firstEntity.getObjectName()).isEqualTo("obj-1");
        assertThat(firstEntity.getName()).isEqualTo("first.txt");
        assertThat(firstEntity.getType()).isEqualTo("text/plain");
        assertThat(firstEntity.getSize()).isEqualTo(first.getSize());
        assertThat(firstEntity.getCreator()).isEqualTo("123");
        assertThat(firstEntity.getUpdater()).isEqualTo("123");
    }

    @Test
    void uploadToTaskLog_rollsBackWhenPersistenceFails() {
        InMemoryFileMapper mapper = new InMemoryFileMapper();
        RecordingGcsFileClient client = new RecordingGcsFileClient();
        FileProviderPropertiesConfig config = new FileProviderPropertiesConfig();
        config.setProvider("gcs");
        FileStorageServiceImpl service =
                new FileStorageServiceImpl(new TestFileClientFactory(client), config, mapper);

        MockMultipartFile first =
                new MockMultipartFile("files", "first.txt", "text/plain", "alpha".getBytes());
        MockMultipartFile second =
                new MockMultipartFile("files", "second.txt", "text/plain", "beta".getBytes());
        FileUploadReqVO req = new FileUploadReqVO();
        req.setTaskLogId(33L);
        req.setEventId(44L);
        req.setFiles(List.of(first, second));

        client.respondWith(
                "first.txt", new FileClient.FileUploadResult("obj-1", "text/plain", 5L, null));
        client.respondWith(
                "second.txt", new FileClient.FileUploadResult("obj-2", "text/plain", 4L, null));

        RuntimeException failure = new RuntimeException("database down");
        mapper.failOnInsert(failure);

        try (StpLogicRestorer ignored = new StpLogicRestorer(777L)) {
            assertThatThrownBy(() -> service.uploadToTaskLog(req)).isSameAs(failure);
        }

        assertThat(client.deletedObjects()).containsExactlyInAnyOrder("obj-1", "obj-2");
    }

    private static final class FixedIdStpLogic extends StpLogic {
        private final Object loginId;

        private FixedIdStpLogic(Object loginId) {
            super(StpUtil.TYPE);
            this.loginId = loginId;
        }

        @Override
        public Object getLoginId() {
            return loginId;
        }
    }

    private static final class StpLogicRestorer implements AutoCloseable {
        private final StpLogic previous;

        private StpLogicRestorer(Object loginId) {
            this.previous = StpUtil.getStpLogic();
            StpUtil.setStpLogic(new FixedIdStpLogic(loginId));
        }

        @Override
        public void close() {
            StpUtil.setStpLogic(previous);
        }
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
            super(storageProxy(), defaultGcsConfig());
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

    private static final class RecordingGcsFileClient extends GcsFileClient {
        private final Map<String, FileClient.FileUploadResult> responses =
                new ConcurrentHashMap<>();
        private final List<String> deleted = Collections.synchronizedList(new ArrayList<>());

        private RecordingGcsFileClient() {
            super(storageProxy(), defaultGcsConfig());
        }

        void respondWith(String fileName, FileClient.FileUploadResult result) {
            responses.put(fileName, result);
        }

        List<String> deletedObjects() {
            return deleted;
        }

        @Override
        public FileUploadResult uploadFile(MultipartFile file) {
            String name = file.getOriginalFilename();
            FileClient.FileUploadResult result = responses.get(name);
            if (result == null) {
                throw new IllegalStateException("No upload result configured for " + name);
            }
            return result;
        }

        @Override
        public String generateSignedUrl(String objectName) {
            return "signed://" + objectName;
        }

        @Override
        public void deleteQuietly(String objectName) {
            deleted.add(objectName);
        }
    }

    private static Storage storageProxy() {
        return (Storage)
                Proxy.newProxyInstance(
                        Storage.class.getClassLoader(),
                        new Class<?>[] {Storage.class},
                        (proxy, method, args) -> null);
    }

    private static GcsPropertiesConfig defaultGcsConfig() {
        GcsPropertiesConfig config = new GcsPropertiesConfig();
        config.setBucket("bucket");
        config.setSignedUrlExpiryMinutes(5);
        return config;
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
        private final Map<Long, FileDO> store = new LinkedHashMap<>();
        private long idSeq = 1;
        private RuntimeException insertFailure;

        void store(FileDO file) {
            if (file.getId() == null) {
                file.setId(idSeq++);
            }
            store.put(file.getId(), file);
        }

        void failOnInsert(RuntimeException failure) {
            this.insertFailure = failure;
        }

        List<FileDO> all() {
            return new ArrayList<>(store.values());
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
            if (insertFailure != null) {
                throw insertFailure;
            }
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
