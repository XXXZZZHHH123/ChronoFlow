package nus.edu.u.system.domain.vo.event;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventPageReqVO {
    private String name;
    private Integer status;
    private Long organizerId; // 按组织者筛选

    private LocalDateTime startTimeFrom;
    private LocalDateTime startTimeTo;

    private Integer pageNo = 1;
    private Integer pageSize = 10;
}
