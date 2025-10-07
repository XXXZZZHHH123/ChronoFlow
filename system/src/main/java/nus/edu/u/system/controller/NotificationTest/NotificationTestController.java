package nus.edu.u.system.controller.NotificationTest;

import lombok.RequiredArgsConstructor;
import nus.edu.u.system.domain.vo.attendee.AttendeeInviteReqVO;
import nus.edu.u.system.domain.vo.reg.RegOrganizerReqVO;
import nus.edu.u.system.domain.vo.reg.RegSearchReqVO;
import nus.edu.u.system.service.email.AttendeeEmailService;
import nus.edu.u.system.service.email.MemberEmailService;
import nus.edu.u.system.service.email.OrganizerEmailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationTestController {

    private final OrganizerEmailService organizerEmailService;
    private final MemberEmailService memberEmailService;
    private final AttendeeEmailService attendeeEmailService;

    @PostMapping(value = "/attendee-invite", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> sendAttendeeInvite(
            @ModelAttribute AttendeeInviteReqVO req
    ) throws Exception {
        String status = attendeeEmailService.sendAttendeeInvite(req);
        HttpStatus http = "ALREADY_ACCEPTED".equals(status) ? HttpStatus.OK : HttpStatus.ACCEPTED;
        return ResponseEntity.status(http).body(Map.of("status", status));
    }

    /**
     * POST /api/v1/notifications/test/organizer/welcome
     *
     * Example request (application/json):
     * {
     *   "name": "Thet Naung Soe",
     *   "username": "thetnaung",
     *   "userEmail": "thet@example.com",
     *   "mobile": "12345678",
     *   "organizationName": "ChronoFlow",
     *   "organizationAddress": "Singapore",
     *   "organizationCode": "CF12345"
     * }
     */
    @PostMapping("/test/organizer/welcome")
    public ResponseEntity<?> sendWelcomeEmail(@RequestBody RegOrganizerReqVO req) {
        String result = organizerEmailService.sendWelcomeEmailOrganizer(req);
        HttpStatus status = "ALREADY_ACCEPTED".equals(result) ? HttpStatus.OK : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(
                Map.of("status", result, "email", req.getUserEmail())
        );
    }

    @PostMapping("/test/member-invite")
    public ResponseEntity<Map<String, String>> testMemberInvite(
            @RequestParam String to,
            @RequestParam Long organizationId,
            @RequestParam Long userId
    ) {
        var req = new RegSearchReqVO(organizationId, userId);
        String result = memberEmailService.sendMemberInviteEmail(to, req);
        return ResponseEntity.accepted().body(Map.of("status", result));
    }
}
