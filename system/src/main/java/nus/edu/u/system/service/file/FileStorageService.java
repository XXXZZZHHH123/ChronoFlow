package nus.edu.u.system.service.file;

import nus.edu.u.system.domain.vo.file.FileUploadReqVO;
import nus.edu.u.system.domain.vo.file.FileResultVO;

import java.util.List;

public interface FileStorageService {
    FileResultVO uploadToTaskLog(FileUploadReqVO req);
    FileResultVO downloadFile(Long fileId);
    List<FileResultVO> downloadFilesByTaskLogId(Long taskLogId);
}