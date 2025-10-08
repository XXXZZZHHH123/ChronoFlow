package nus.edu.u.system.domain.vo.file;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class FileUploadReqVO {

    @NotNull(message = "taskLogId is required")
    private Long taskLogId;

    @NotNull(message = "eventId is required")
    private Long eventId;

    @NotEmpty(message = "files must not be empty")
    private List<MultipartFile> files;
}
