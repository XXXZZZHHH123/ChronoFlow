package nus.edu.u.system.domain.vo.file;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileUploadReqVO {

    @NotNull(message = "taskLogId is required")
    private Long taskLogId;

    @NotNull(message = "eventId is required")
    private Long eventId;

    @NotNull(message = "File cannot be null")
    private MultipartFile file;

}