package nus.edu.u.system.service.email;


import nus.edu.u.system.domain.vo.reg.RegOrganizerReqVO;

public interface OrganizerEmailService {
    /**
     * Sends the “Organizer Welcome” email and returns the request id
     * (or "ALREADY_ACCEPTED" if idempotency hits).
     */
    String sendWelcomeEmailOrganizer(RegOrganizerReqVO req);
}
