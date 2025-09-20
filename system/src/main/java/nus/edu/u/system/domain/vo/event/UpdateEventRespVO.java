package nus.edu.u.system.domain.vo.event;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UpdateEventRespVO {
    private Long id;
    private String eventName;
    private String description;

    private Long organizerId;
    private List<Long> participantUserIds;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private Integer status;
    private String remarks;

    private LocalDateTime updateTime;
}
