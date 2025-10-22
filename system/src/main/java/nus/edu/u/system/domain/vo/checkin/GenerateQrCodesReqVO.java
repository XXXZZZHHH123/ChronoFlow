package nus.edu.u.system.domain.vo.checkin;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;
import nus.edu.u.system.domain.vo.attendee.AttendeeReqVO;

@Data
public class GenerateQrCodesReqVO {
    @NotNull(message = "Event ID cannot be null")
    private Long eventId;

    @NotEmpty(message = "Attendee list cannot be empty")
    private List<AttendeeReqVO> attendees;

    private Integer qrSize = 400;
}
