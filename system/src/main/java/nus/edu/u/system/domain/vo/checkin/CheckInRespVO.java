package nus.edu.u.system.domain.vo.checkin;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CheckInRespVO {
    private Long eventId;

    private String eventName;

    private Long userId;

    private String userName;

    private LocalDateTime checkInTime;

    private String message;

    private Boolean success;
}
