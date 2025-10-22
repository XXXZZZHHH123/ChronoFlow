package nus.edu.u.system.domain.vo.task;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** Task response view object used by task CRUD operations. */
@Data
public class TaskRespVO {
    private Long id;
    private Long eventId;
    private String name;
    private String description;
    private Integer status;
    private String remark;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private AssignerUserVO assignerUser;
    private AssignedUserVO assignedUser;
    private EventVO event;

    @Data
    public static class AssignerUserVO {
        private Long id;
        private String name;
        private String email;
        private String phone;
        private List<GroupVO> groups;

        @Data
        public static class GroupVO {
            private Long id;
            private String name;
        }
    }

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
