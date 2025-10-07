package nus.edu.u.system.service.email;

import nus.edu.u.system.domain.vo.attendee.AttendeeInviteReqVO;

public interface AttendeeEmailService {
    String sendAttendeeInvite(AttendeeInviteReqVO req);
}
