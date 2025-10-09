package nus.edu.u.system.domain.vo.task;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * Dashboard-oriented task response containing aggregated event and assignee information.
 *
 * <p>Dedicated to dashboard views. For CRUD operations use {@link TaskRespVO}.
 */
@Data
public class TasksRespVO {
    private Long id;
    private String name;
    private String description;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private AssignedUserVO assignedUser;
    private EventVO event;

    @Data
    public static class AssignedUserVO {
        private Long id;
        private String name;
        private String email;
        private String phone;
        private List<GroupVO> groups;

        @Data
        public static class GroupVO {
            private Long id;
            private String name;
            private Long eventId;
            private Long leadUserId;
            private String remark;
        }
    }

    @Data
    public static class EventVO {
        private Long id;
        private String name;
        private String description;
        private Long organizerId;
        private String location;
        private Integer status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String remark;
    }
}
