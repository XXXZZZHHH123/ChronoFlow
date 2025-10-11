package nus.edu.u.system.domain.vo.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class EventCreateReqVO {
    @NotBlank(message = "eventName cannot be empty")
    @JsonProperty("name")
    private String eventName;

    private String description;

    private Long organizerId;

    private List<Long> participantUserIds;

    @NotNull(message = "startTime cannot be empty")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime startTime;

    private String location;

    @NotNull(message = "endTime cannot be empty")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime endTime;

    private Integer status;

    @JsonProperty("remark")
    private String remarks;
}
