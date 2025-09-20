package nus.edu.u.system.domain.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDTO {
    private Long id;
    private String eventName;
    private String description;

    private Long organizerId;
    private List<Long> participantUserIds;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private Integer status;
    private String remarks;
}
