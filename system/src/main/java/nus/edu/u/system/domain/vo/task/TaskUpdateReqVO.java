package nus.edu.u.system.domain.vo.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Data;
import nus.edu.u.common.annotation.InEnum;
import nus.edu.u.system.enums.task.TaskActionEnum;

@Data
public class TaskUpdateReqVO {

    private String name;

    private String description;

    @InEnum(value = TaskActionEnum.class, message = "Wrong action type")
    private Integer type;

    private Long targetUserId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private LocalDateTime endTime;
}
