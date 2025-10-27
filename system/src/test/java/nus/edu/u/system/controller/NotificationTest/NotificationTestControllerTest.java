package nus.edu.u.system.controller.NotificationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Map;
import nus.edu.u.system.domain.vo.attendee.AttendeeInviteReqVO;
import nus.edu.u.system.domain.vo.reg.RegOrganizerReqVO;
import nus.edu.u.system.domain.vo.reg.RegSearchReqVO;
import nus.edu.u.system.service.email.AttendeeEmailService;
import nus.edu.u.system.service.email.MemberEmailService;
import nus.edu.u.system.service.email.OrganizerEmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class NotificationTestControllerTest {

    @Mock private OrganizerEmailService organizerEmailService;
    @Mock private MemberEmailService memberEmailService;
    @Mock private AttendeeEmailService attendeeEmailService;

    @InjectMocks private NotificationTestController notificationTestController;

    @Test
    void sendAttendeeInvite_returnsAcceptedWhenPending() throws Exception {
        when(attendeeEmailService.sendAttendeeInvite(any(AttendeeInviteReqVO.class)))
                .thenReturn("PENDING");

        ResponseEntity<Map<String, String>> response =
                notificationTestController.sendAttendeeInvite(new AttendeeInviteReqVO());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).containsEntry("status", "PENDING");
    }

    @Test
    void sendAttendeeInvite_returnsOkWhenAlreadyAccepted() throws Exception {
        when(attendeeEmailService.sendAttendeeInvite(any(AttendeeInviteReqVO.class)))
                .thenReturn("ALREADY_ACCEPTED");

        ResponseEntity<Map<String, String>> response =
                notificationTestController.sendAttendeeInvite(new AttendeeInviteReqVO());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "ALREADY_ACCEPTED");
    }

    @Test
    void sendWelcomeEmail_returnsStatusPayload() {
        RegOrganizerReqVO reqVO =
                RegOrganizerReqVO.builder()
                        .name("Org")
                        .username("organizer")
                        .userPassword("password123")
                        .userEmail("org@example.com")
                        .mobile("98887777")
                        .organizationName("Org Inc")
                        .build();
        when(organizerEmailService.sendWelcomeEmailOrganizer(reqVO)).thenReturn("SENT");

        ResponseEntity<?> response = notificationTestController.sendWelcomeEmail(reqVO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) response.getBody()).get("email")).isEqualTo("org@example.com");
    }

    @Test
    void testMemberInvite_returnsAcceptedResponse() {
        when(memberEmailService.sendMemberInviteEmail(
                        eq("user@example.com"), any(RegSearchReqVO.class)))
                .thenReturn("QUEUED");

        ResponseEntity<Map<String, String>> response =
                notificationTestController.testMemberInvite("user@example.com", 1L, 2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).containsEntry("status", "QUEUED");
    }
}
