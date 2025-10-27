package nus.edu.u.system.service.email;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import nus.edu.u.system.domain.dto.NotificationRequestDTO;
import nus.edu.u.system.enums.email.NotificationChannel;
import org.junit.jupiter.api.Test;

class NotificationServiceImplTest {

    @Test
    void send_usesMatchingSender() {
        RecordingSender emailSender = new RecordingSender(NotificationChannel.EMAIL, "email-id");
        NotificationSender pushSender = new RecordingSender(NotificationChannel.PUSH, "push-id");

        NotificationServiceImpl service =
                new NotificationServiceImpl(List.of(pushSender, emailSender));

        NotificationRequestDTO request =
                NotificationRequestDTO.builder()
                        .channel(NotificationChannel.EMAIL)
                        .to("user@example.com")
                        .templateId("template")
                        .variables(Map.of())
                        .locale(Locale.ENGLISH)
                        .build();

        String result = service.send(request);

        assertThat(result).isEqualTo("email-id");
        assertThat(emailSender.lastRequest).isSameAs(request);
    }

    @Test
    void send_whenNoSenderThrows() {
        NotificationServiceImpl service =
                new NotificationServiceImpl(
                        List.of(new RecordingSender(NotificationChannel.PUSH, "push")));

        NotificationRequestDTO request =
                NotificationRequestDTO.builder()
                        .channel(NotificationChannel.EMAIL)
                        .to("user@example.com")
                        .templateId("template")
                        .build();

        assertThatThrownBy(() -> service.send(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No sender for channel");
    }

    private static final class RecordingSender implements NotificationSender {
        private final NotificationChannel channel;
        private final String result;
        NotificationRequestDTO lastRequest;

        private RecordingSender(NotificationChannel channel, String result) {
            this.channel = channel;
            this.result = result;
        }

        @Override
        public boolean supports(NotificationChannel channel) {
            return this.channel == channel;
        }

        @Override
        public String send(NotificationRequestDTO request) {
            lastRequest = request;
            return result;
        }
    }
}
