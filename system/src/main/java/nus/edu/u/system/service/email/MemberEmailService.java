package nus.edu.u.system.service.email;


import nus.edu.u.system.domain.vo.reg.RegSearchReqVO;

public interface MemberEmailService {
    String sendMemberInviteEmail(String recipientEmail, RegSearchReqVO req);
}
