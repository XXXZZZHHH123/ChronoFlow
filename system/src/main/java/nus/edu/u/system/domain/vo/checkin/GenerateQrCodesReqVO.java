package nus.edu.u.system.domain.vo.checkin;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class GenerateQrCodesReqVO {
    @NotNull(message = "Event ID cannot be null")
    private Long eventId;

    @NotEmpty(message = "Attendee list cannot be empty")
    private List<AttendeeInfo> attendees;

    @Data
    public static class AttendeeInfo {
        private String email;
        private String name;
        private String mobile;
    }

    private Integer qrSize = 400;
}
