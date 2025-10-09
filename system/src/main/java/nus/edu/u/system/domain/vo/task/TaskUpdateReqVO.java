package nus.edu.u.system.domain.vo.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;
import nus.edu.u.common.annotation.InEnum;
import nus.edu.u.system.enums.task.TaskActionEnum;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

@Data
public class TaskUpdateReqVO {

    private String name;

    private String description;

    @InEnum(value = TaskActionEnum.class, message = "Wrong action type")
    private Integer type;

    private Long targetUserId;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private LocalDateTime endTime;

    private List<MultipartFile> files;
}
