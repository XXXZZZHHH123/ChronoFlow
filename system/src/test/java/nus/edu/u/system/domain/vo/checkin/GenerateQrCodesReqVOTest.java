package nus.edu.u.system.domain.vo.checkin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nus.edu.u.system.domain.vo.attendee.AttendeeReqVO;
import org.junit.jupiter.api.Test;

class GenerateQrCodesReqVOTest {

    @Test
    void defaultsAndAttendeesAreRetained() {
        GenerateQrCodesReqVO reqVO = new GenerateQrCodesReqVO();
        assertThat(reqVO.getQrSize()).isEqualTo(400);

        AttendeeReqVO attendee = new AttendeeReqVO();
        attendee.setEmail("test@example.com");
        attendee.setName("Test");
        attendee.setMobile("12345678");

        reqVO.setEventId(11L);
        reqVO.setAttendees(List.of(attendee));
        reqVO.setQrSize(512);

        assertThat(reqVO.getEventId()).isEqualTo(11L);
        assertThat(reqVO.getAttendees()).containsExactly(attendee);
        assertThat(reqVO.getQrSize()).isEqualTo(512);
    }
}
