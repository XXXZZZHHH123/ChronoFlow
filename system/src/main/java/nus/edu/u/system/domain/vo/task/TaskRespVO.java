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
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private AssignedUserVO assignedUser;
    private EventSummaryVO event;

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
    public static class EventSummaryVO {
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
