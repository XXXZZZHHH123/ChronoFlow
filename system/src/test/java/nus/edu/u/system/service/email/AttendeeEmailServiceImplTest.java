package nus.edu.u.system.service.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import nus.edu.u.system.domain.dto.AttachmentDTO;
import nus.edu.u.system.domain.dto.NotificationRequestDTO;
import nus.edu.u.system.domain.vo.attendee.AttendeeInviteReqVO;
import nus.edu.u.system.enums.email.NotificationChannel;
import org.junit.jupiter.api.Test;

class AttendeeEmailServiceImplTest {

    @Test
    void sendAttendeeInvite_buildsNotificationRequest() {
        RecordingNotificationService notificationService = new RecordingNotificationService();
        AttendeeEmailServiceImpl service = new AttendeeEmailServiceImpl(notificationService);

        AttendeeInviteReqVO req =
                AttendeeInviteReqVO.builder()
                        .toEmail("user@example.com")
                        .attendeeName("User")
                        .organizationName("Org")
                        .eventName("Expo")
                        .eventDate("2024-10-01")
                        .eventLocation("Hall A")
                        .eventDescription("Welcome!")
                        .qrCodeBytes(new byte[] {1, 2, 3})
                        .qrCodeContentType("image/png")
                        .build();

        String result = service.sendAttendeeInvite(req);

        assertThat(result).isEqualTo("queued");
        assertThat(notificationService.lastRequest.to()).isEqualTo("user@example.com");
        assertThat(notificationService.lastRequest.channel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(notificationService.lastRequest.templateId()).isEqualTo("attendee-qr-invite");
        assertThat(notificationService.lastRequest.locale()).isEqualTo(Locale.ENGLISH);
        assertThat(notificationService.lastRequest.variables().get("attendeeName"))
                .isEqualTo("User");

        List<AttachmentDTO> attachments = notificationService.lastRequest.attachments();
        assertThat(attachments).hasSizeGreaterThanOrEqualTo(1);
        assertThat(attachments.get(0).contentId()).isNotNull();
    }

    @Test
    void sendAttendeeInvite_withoutQrCodeStillSends() {
        RecordingNotificationService notificationService = new RecordingNotificationService();
        AttendeeEmailServiceImpl service = new AttendeeEmailServiceImpl(notificationService);

        AttendeeInviteReqVO req =
                AttendeeInviteReqVO.builder()
                        .toEmail("user@example.com")
                        .attendeeName("User")
                        .organizationName("Org")
                        .eventName("Expo")
                        .eventDate("2024-10-01")
                        .eventLocation("Hall A")
                        .build();

        service.sendAttendeeInvite(req);

        assertThat(notificationService.lastRequest.attachments()).isNotNull();
    }

    private static final class RecordingNotificationService implements NotificationService {
        NotificationRequestDTO lastRequest;

        @Override
        public String send(NotificationRequestDTO req) {
            lastRequest = req;
            return "queued";
        }
    }
}
