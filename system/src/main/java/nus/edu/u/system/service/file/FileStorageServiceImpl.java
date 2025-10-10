package nus.edu.u.system.service.file;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import nus.edu.u.framework.file.FileProviderPropertiesConfig;
import nus.edu.u.system.domain.dataobject.file.FileDO;
import nus.edu.u.system.domain.vo.file.FileResultVO;
import nus.edu.u.system.domain.vo.file.FileUploadReqVO;
import nus.edu.u.system.mapper.file.FileMapper;
import nus.edu.u.system.provider.file.FileClient;
import nus.edu.u.system.provider.file.FileClientFactory;
import nus.edu.u.system.provider.file.GcsFileClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final FileClientFactory fileClientFactory;
    private final FileProviderPropertiesConfig providerConfig;
    private final FileMapper fileMapper;

    @Override
    @Transactional
    public List<FileResultVO> uploadToTaskLog(FileUploadReqVO req) {

        if (req.getFiles() == null || req.getFiles().isEmpty()) {
            throw new IllegalArgumentException("files must not be empty");
        }

        final String provider = providerConfig.getProvider();
        final FileClient client = fileClientFactory.create(provider);
        final boolean isGcs = client instanceof GcsFileClient;
        final GcsFileClient gcs = isGcs ? (GcsFileClient) client : null;

        final int n = req.getFiles().size();
        final int threads = Math.min(8, Math.max(2, n));

        final ExecutorService pool = Executors.newFixedThreadPool(threads);

        final List<String> uploadedObjectNames = Collections.synchronizedList(new ArrayList<>());

        try {

            List<CompletableFuture<FileClient.FileUploadResult>> futures =
                    req.getFiles().stream()
                            .filter(f -> f != null && !f.isEmpty())
                            .map(
                                    file ->
                                            CompletableFuture.supplyAsync(
                                                    () -> {
                                                        FileClient.FileUploadResult r =
                                                                client.uploadFile(file);
                                                        uploadedObjectNames.add(r.objectName());
                                                        return r;
                                                    },
                                                    pool))
                            .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            List<FileClient.FileUploadResult> uploaded =
                    futures.stream().map(CompletableFuture::join).toList();

            List<FileDO> batchEntities = new ArrayList<>(uploaded.size());
            for (int i = 0; i < uploaded.size(); i++) {
                MultipartFile file = req.getFiles().get(i);
                FileClient.FileUploadResult r = uploaded.get(i);

                FileDO fileDO =
                        FileDO.builder()
                                .taskLogId(req.getTaskLogId())
                                .eventId(req.getEventId())
                                .provider(provider)
                                .name(file.getOriginalFilename())
                                .objectName(r.objectName())
                                .type(r.contentType())
                                .size(r.size())
                                .build();
                fileDO.setCreator(StpUtil.getLoginId().toString());
                fileDO.setUpdater(StpUtil.getLoginId().toString());
                fileDO.setCreateTime(LocalDateTime.now());
                fileDO.setUpdateTime(LocalDateTime.now());
                batchEntities.add(fileDO);
            }

            if (!batchEntities.isEmpty()) {
                fileMapper.insertBatch(batchEntities);
            }

            List<FileResultVO> results = new ArrayList<>(uploaded.size());
            for (FileClient.FileUploadResult r : uploaded) {
                String signedUrl = isGcs ? gcs.generateSignedUrl(r.objectName()) : null;
                results.add(
                        FileResultVO.builder()
                                .objectName(r.objectName())
                                .contentType(r.contentType())
                                .size(r.size())
                                .signedUrl(signedUrl)
                                .build());
            }
            return results;

        } catch (RuntimeException ex) {

            for (String name : uploadedObjectNames) {
                client.deleteQuietly(name);
            }
            throw ex;
        } finally {
            pool.shutdown();
        }
    }

    @Override
    public FileResultVO downloadFile(Long taskLogId) {
        FileDO file =
                fileMapper.selectOne(
                        new LambdaQueryWrapper<FileDO>()
                                .eq(FileDO::getTaskLogId, taskLogId)
                                .last("LIMIT 1"));
        if (file == null)
            throw new IllegalArgumentException("File not found for taskLogId: " + taskLogId);

        FileClient client = fileClientFactory.create(file.getProvider());

        if (client instanceof GcsFileClient gcs) {
            String signedUrl = gcs.generateSignedUrl(file.getObjectName());
            return FileResultVO.builder()
                    .objectName(file.getObjectName())
                    .contentType(file.getType())
                    .size(file.getSize())
                    .signedUrl(signedUrl)
                    .build();
        }

        throw new UnsupportedOperationException(
                "Signed URL generation not supported for provider: " + file.getProvider());
    }

    @Override
    public List<FileResultVO> downloadFilesByTaskLogId(Long taskLogId) {
        List<FileDO> files =
                fileMapper.selectList(
                        new LambdaQueryWrapper<FileDO>().eq(FileDO::getTaskLogId, taskLogId));

        if (files.isEmpty()) return Collections.emptyList();

        FileClient client = fileClientFactory.create(files.get(0).getProvider());

        if (client instanceof GcsFileClient gcs) {
            return files.stream()
                    .map(
                            f ->
                                    FileResultVO.builder()
                                            .objectName(f.getObjectName())
                                            .contentType(f.getType())
                                            .size(f.getSize())
                                            .signedUrl(gcs.generateSignedUrl(f.getObjectName()))
                                            .build())
                    .toList();
        }

        throw new UnsupportedOperationException(
                "Signed URL generation not supported for this provider");
    }
}
