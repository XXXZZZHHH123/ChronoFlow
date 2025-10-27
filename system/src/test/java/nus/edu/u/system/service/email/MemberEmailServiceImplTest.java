package nus.edu.u.system.service.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Map;
import nus.edu.u.system.domain.dto.AttachmentDTO;
import nus.edu.u.system.domain.dto.NotificationRequestDTO;
import nus.edu.u.system.domain.vo.reg.RegSearchReqVO;
import nus.edu.u.system.enums.email.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MemberEmailServiceImplTest {

    private MemberEmailServiceImpl service;
    private RecordingNotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new RecordingNotificationService();
        service = new MemberEmailServiceImpl(notificationService);
    }

    @Test
    void sendMemberInviteEmail_buildsRequestWithVariables() {
        RegSearchReqVO req = RegSearchReqVO.builder().organizationId(5L).userId(9L).build();

        String result = service.sendMemberInviteEmail("invitee@example.com", req);

        assertThat(result).isEqualTo("queued");

        NotificationRequestDTO captured = notificationService.lastRequest;
        assertThat(captured.channel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(captured.to()).isEqualTo("invitee@example.com");
        assertThat(captured.templateId()).isEqualTo("member-invite");
        assertThat(captured.locale()).isEqualTo(Locale.ENGLISH);

        Map<String, Object> vars = captured.variables();
        assertThat(vars.get("organizationId")).isEqualTo(5L);
        assertThat(vars.get("userId")).isEqualTo(9L);
        assertThat(vars.get("inviteUrl"))
                .asString()
                .contains("organisation_id=5")
                .contains("user_id=9");
        assertThat(vars.get("logoCid")).isEqualTo("logo");
    }

    private static final class RecordingNotificationService implements NotificationService {
        NotificationRequestDTO lastRequest;

        @Override
        public String send(NotificationRequestDTO request) {
            this.lastRequest = request;
            // Ensure attachments list is safe to iterate
            if (request.attachments() != null) {
                for (AttachmentDTO ignored : request.attachments()) {}
            }
            return "queued";
        }
    }
}
