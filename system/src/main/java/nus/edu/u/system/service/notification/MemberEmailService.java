package nus.edu.u.system.service.notification;

import nus.edu.u.system.domain.vo.reg.RegSearchReqVO;

public interface MemberEmailService {
    void sendMemberInviteEmail(String recipientEmail, RegSearchReqVO req);
}
