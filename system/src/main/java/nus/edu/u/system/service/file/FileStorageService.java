package nus.edu.u.system.service.file;

import java.util.List;
import nus.edu.u.system.domain.vo.file.FileResultVO;
import nus.edu.u.system.domain.vo.file.FileUploadReqVO;

public interface FileStorageService {
    List<FileResultVO> uploadToTaskLog(FileUploadReqVO req);

    FileResultVO downloadFile(Long fileId);
    List<FileResultVO> downloadFilesByTaskLogId(Long taskLogId);
}
