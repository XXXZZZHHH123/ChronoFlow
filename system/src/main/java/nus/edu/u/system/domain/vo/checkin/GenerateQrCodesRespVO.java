package nus.edu.u.system.domain.vo.checkin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nus.edu.u.system.domain.vo.attendee.AttendeeQrCodeRespVO;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GenerateQrCodesRespVO {
    private Long eventId;

    private String eventName;

    private Integer totalCount;

    private List<AttendeeQrCodeRespVO> attendees;
}
