package nus.edu.u.system.controller.FileTest;

import lombok.RequiredArgsConstructor;
import nus.edu.u.system.domain.vo.file.FileResultVO;
import nus.edu.u.system.domain.vo.file.FileUploadReqVO;
import nus.edu.u.system.service.file.FileStorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Validated
public class FileTestController {

    private final FileStorageService fileStorageService;

    /** Upload a file linked to a specific task log */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileResultVO> upload(@ModelAttribute @Validated FileUploadReqVO req) {
        return ResponseEntity.ok(fileStorageService.uploadToTaskLog(req));
    }

    /** Download a single file by taskLogId */
    @GetMapping("/{taskLogId}/download")
    public FileResultVO downloadFileByTaskLogId(@PathVariable("taskLogId") Long taskLogId) {
        return fileStorageService.downloadFile(taskLogId);
    }

    /** Download all files attached to a taskLogId */
    @GetMapping("/{taskLogId}/download-all")
    public List<FileResultVO> downloadFilesByTaskLogId(@PathVariable("taskLogId") Long taskLogId) {
        return fileStorageService.downloadFilesByTaskLogId(taskLogId);
    }
}