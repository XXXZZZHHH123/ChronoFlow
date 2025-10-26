package nus.edu.u.system.service.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Map;
import nus.edu.u.system.domain.dto.NotificationRequestDTO;
import nus.edu.u.system.domain.vo.reg.RegOrganizerReqVO;
import nus.edu.u.system.enums.email.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrganizerEmailServiceImplTest {

    private OrganizerEmailServiceImpl service;
    private RecordingNotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new RecordingNotificationService();
        service = new OrganizerEmailServiceImpl(notificationService);
    }

    @Test
    void sendWelcomeEmailOrganizer_buildsRequest() {
        RegOrganizerReqVO req =
                RegOrganizerReqVO.builder()
                        .name("Alice")
                        .username("alice123")
                        .userEmail("alice@example.com")
                        .mobile("12345678")
                        .organizationName("ChronoFlow")
                        .organizationAddress("123 Street")
                        .organizationCode("CF001")
                        .build();

        String result = service.sendWelcomeEmailOrganizer(req);

        assertThat(result).isEqualTo("queued");

        NotificationRequestDTO request = notificationService.lastRequest;
        assertThat(request.channel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(request.to()).isEqualTo("alice@example.com");
        assertThat(request.templateId()).isEqualTo("welcome-email-organizer");
        assertThat(request.locale()).isEqualTo(Locale.ENGLISH);

        Map<String, Object> vars = request.variables();
        assertThat(vars.get("name")).isEqualTo("Alice");
        assertThat(vars.get("organizationCode")).isEqualTo("CF001");
        assertThat(vars.get("logoCid")).isEqualTo("logo");
    }

    private static final class RecordingNotificationService implements NotificationService {
        NotificationRequestDTO lastRequest;

        @Override
        public String send(NotificationRequestDTO request) {
            this.lastRequest = request;
            return "queued";
        }
    }
}
