package nus.edu.u.system.domain.vo.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Data;
import nus.edu.u.common.annotation.InEnum;
import nus.edu.u.system.enums.task.TaskStatusEnum;

@Data
public class TaskUpdateReqVO {

    private String name;

    private String description;

    @InEnum(value = TaskStatusEnum.class, message = "Illegal task status")
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private LocalDateTime endTime;

    private Long assignedUserId;
}
