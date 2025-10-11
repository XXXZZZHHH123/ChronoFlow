package nus.edu.u.system.domain.vo.attendee;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttendeeInfoRespVO {
    private String eventName;
    private String attendeeName;
    private String attendeeEmail;
    private Integer checkInStatus;
    private LocalDateTime checkInTime;
    private String message;
}
