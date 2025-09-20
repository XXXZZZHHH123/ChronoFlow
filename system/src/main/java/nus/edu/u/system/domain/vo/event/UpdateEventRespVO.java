package nus.edu.u.system.domain.vo.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class UpdateEventRespVO {
    private Long id;

    @JsonProperty("name")
    private String eventName;

    private String description;

    private Long organizerId;
    private List<Long> participantUserIds;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private Integer status;
    private String remarks;

    private LocalDateTime updateTime;
}
