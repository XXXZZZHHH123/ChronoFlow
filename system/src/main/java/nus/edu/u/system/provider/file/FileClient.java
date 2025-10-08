package nus.edu.u.system.provider.file;

import org.springframework.web.multipart.MultipartFile;

public interface FileClient {
    record FileUploadResult(String objectName, String contentType, long size, String signedUrl) {}
    FileUploadResult uploadFile(MultipartFile file);
}