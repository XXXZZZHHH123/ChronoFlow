package nus.edu.u.system.domain.vo.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TaskCreateReqVO {
    @NotBlank(message = "name cannot be empty")
    private String name;

    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private LocalDateTime endTime;

    @NotNull(message = "You should assign task to a member")
    private Long targetUserId;
}
