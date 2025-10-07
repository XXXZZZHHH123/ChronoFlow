package nus.edu.u.system.service.checkin;

import nus.edu.u.system.domain.vo.checkin.CheckInRespVO;
import nus.edu.u.system.domain.vo.checkin.GenerateQrCodesReqVO;
import nus.edu.u.system.domain.vo.checkin.GenerateQrCodesRespVO;

public interface CheckInService {
    /**
     * Perform check-in with token
     *
     * @param token Check-in token
     * @return Check-in result
     */
    CheckInRespVO checkIn(String token);

    /**
     * Generate check-in tokens and QR codes for multiple attendees Returns QR code data that can be
     * used by email service
     *
     * @param reqVO Request with eventId and userIds
     * @return QR codes for all attendees
     */
    GenerateQrCodesRespVO generateQrCodesForAttendees(GenerateQrCodesReqVO reqVO);

    /**
     * Get check-in token for a specific attendee
     *
     * @param eventId Event ID
     * @param email Attendee email
     * @return Check-in token
     */
    String getCheckInToken(Long eventId, String email);
}
