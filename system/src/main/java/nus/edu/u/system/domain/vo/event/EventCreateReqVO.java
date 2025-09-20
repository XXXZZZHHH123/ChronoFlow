package nus.edu.u.system.domain.vo.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EventCreateReqVO {
    @NotBlank(message = "eventName cannot be empty")
    @JsonProperty("name")
    private String eventName;

    private String description;

    @NotNull(message = "organizerId cannot be empty")
    private Long organizerId;

    private List<Long> participantUserIds;

    @NotNull(message = "startTime cannot be empty")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @NotNull(message = "endTime cannot be empty")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    private Integer status;

    private String remarks;
}
