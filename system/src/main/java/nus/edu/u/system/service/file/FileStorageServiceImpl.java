package nus.edu.u.system.service.file;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.Valid;
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

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final FileClientFactory fileClientFactory;
    private final FileProviderPropertiesConfig providerConfig;
    private final FileMapper fileMapper;

    @Override
    @Transactional
    public FileResultVO uploadToTaskLog(@Valid FileUploadReqVO req) {
        FileClient client = fileClientFactory.create(providerConfig.getProvider());
        FileClient.FileUploadResult r = client.uploadFile(req.getFile());

        FileDO entity = FileDO.builder()
                .taskLogId(req.getTaskLogId())
                .eventId(req.getEventId())
                .provider(providerConfig.getProvider())
                .name(req.getFile().getOriginalFilename())
                .objectName(r.objectName())
                .type(r.contentType())
                .size(r.size())
                .build();

        fileMapper.insert(entity);

        // Return with temporary signed URL for immediate use
        if (client instanceof GcsFileClient gcs) {
            String signedUrl = gcs.generateSignedUrl(r.objectName());
            return FileResultVO.builder()
                    .objectName(r.objectName())
                    .contentType(r.contentType())
                    .size(r.size())
                    .signedUrl(signedUrl)
                    .build();
        }

        return FileResultVO.builder()
                .objectName(r.objectName())
                .contentType(r.contentType())
                .size(r.size())
                .signedUrl(null)
                .build();
    }

    @Override
    public FileResultVO downloadFile(Long taskLogId) {
        FileDO file = fileMapper.selectOne(
                new LambdaQueryWrapper<FileDO>()
                        .eq(FileDO::getTaskLogId, taskLogId)
                        .last("LIMIT 1")
        );
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
        List<FileDO> files = fileMapper.selectList(
                new LambdaQueryWrapper<FileDO>().eq(FileDO::getTaskLogId, taskLogId)
        );

        if (files.isEmpty())
            throw new IllegalArgumentException("No files found for taskLogId: " + taskLogId);

        FileClient client = fileClientFactory.create(files.get(0).getProvider());

        if (client instanceof GcsFileClient gcs) {
            return files.stream()
                    .map(f -> FileResultVO.builder()
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