package nus.edu.u.system.domain.vo.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class EventUpdateReqVO {
    private Long organizerId;

    @JsonProperty("name")
    private String eventName;

    private String description;

    private Integer status;
    private String location;
    private List<Long> participantUserIds;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private String remark;
}
