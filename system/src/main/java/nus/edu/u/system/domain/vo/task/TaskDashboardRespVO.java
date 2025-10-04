package nus.edu.u.system.domain.vo.task;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * Task dashboard response aggregating member profile, group detail, and assigned tasks.
 */
@Data
public class TaskDashboardRespVO {
    private MemberVO member;
    private List<GroupVO> groups;
    private List<TaskRespVO> tasks;

    @Data
    public static class MemberVO {
        private Long id;
        private String username;
        private String email;
        private String phone;
        private Integer status;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }

    @Data
    public static class GroupVO {
        private Long id;
        private String name;
        private Integer sort;
        private Long leadUserId;
        private String remark;
        private Integer status;
        private EventVO event;

        @Data
        public static class EventVO {
            private Long id;
            private String name;
            private String description;
            private String location;
            private Integer status;
            private LocalDateTime startTime;
            private LocalDateTime endTime;
            private String remark;
        }
    }
}
