package nus.edu.u.system.domain.vo.attendee;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

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
