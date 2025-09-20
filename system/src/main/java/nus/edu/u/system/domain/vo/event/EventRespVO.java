package nus.edu.u.system.domain.vo.event;

import lombok.Data;
import nus.edu.u.common.annotation.InEnum;
import nus.edu.u.system.enums.event.EventStatusEnum;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EventRespVO {
    private Long id;
    private String name;
    private String description;

    private Long organizerId;
    private List<Long> participantUserIds;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @InEnum(value = EventStatusEnum.class, message = "Illegal event status")
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
}
