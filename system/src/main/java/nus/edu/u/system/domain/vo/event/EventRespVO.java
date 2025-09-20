package nus.edu.u.system.domain.vo.event;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import nus.edu.u.common.annotation.InEnum;
import nus.edu.u.system.enums.event.EventStatusEnum;

@Data
public class EventRespVO {
    private Long id;
    private String name;
    private String description;
    private Long organizerId;
    private Integer joiningParticipants;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @InEnum(value = EventStatusEnum.class, message = "Illegal event status")
    private Integer status;

    private String remark;
    private LocalDateTime createTime;

    private List<GroupVO> groups;
    private TaskStatusVO taskStatus;

    @Data
    public static class GroupVO {
        private String id;
        private String name;
    }

    @Data
    public static class TaskStatusVO {
        private Integer total;
        private Integer remaining;
        private Integer completed;
    }
}
