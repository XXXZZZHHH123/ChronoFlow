package nus.edu.u.system.controller.file;

import java.util.List;
import lombok.RequiredArgsConstructor;
import nus.edu.u.system.domain.vo.file.FileResultVO;
import nus.edu.u.system.domain.vo.file.FileUploadReqVO;
import nus.edu.u.system.service.file.FileStorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Validated
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<FileResultVO>> uploadToTaskLog(
            @ModelAttribute @Validated FileUploadReqVO req) {
        List<FileResultVO> results = fileStorageService.uploadToTaskLog(req);
        return ResponseEntity.ok(results);
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
