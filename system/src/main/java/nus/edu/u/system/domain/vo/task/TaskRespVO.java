package nus.edu.u.system.domain.vo.task;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class TaskRespVO {
    private Long id;
    private String name;
    private String description;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AssignedUserVO assignedUser;

    @Data
    public static class AssignedUserVO {
        private Long id;
        private String name;
        private List<String> positionTitles;
    }
}
