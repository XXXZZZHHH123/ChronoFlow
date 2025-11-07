package nus.edu.u.system.service.notification;

import nus.edu.u.system.domain.vo.attendee.AttendeeInviteReqVO;

public interface AttendeeEmailService {
    void sendAttendeeInvite(AttendeeInviteReqVO req);
}
