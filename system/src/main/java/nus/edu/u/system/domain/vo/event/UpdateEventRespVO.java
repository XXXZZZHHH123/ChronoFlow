package nus.edu.u.system.domain.vo.event;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'",
            timezone = "UTC")
    private LocalDateTime startTime;

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'",
            timezone = "UTC")
    private LocalDateTime endTime;

    private Integer status;
    private String remarks;

    private LocalDateTime updateTime;
}
