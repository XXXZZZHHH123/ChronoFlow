package nus.edu.u.system.service.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import nus.edu.u.system.domain.dto.AttachmentDTO;
import nus.edu.u.system.domain.dto.NotificationRequestDTO;
import nus.edu.u.system.domain.dto.RenderedTemplateDTO;
import nus.edu.u.system.enums.email.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmailNotificationSenderTest {

    private RecordingTemplateService templateService;
    private RecordingEmailService emailService;
    private EmailNotificationSender sender;

    @BeforeEach
    void setUp() {
        templateService = new RecordingTemplateService();
        emailService = new RecordingEmailService();
        sender = new EmailNotificationSender(templateService, emailService);
    }

    @Test
    void supports_onlyEmailChannel() {
        assertThat(sender.supports(NotificationChannel.EMAIL)).isTrue();
        assertThat(sender.supports(NotificationChannel.PUSH)).isFalse();
    }

    @Test
    void send_rendersTemplateAndDelegatesToEmailService() {
        templateService.rendered =
                RenderedTemplateDTO.builder()
                        .subject("Subject")
                        .bodyHtml("<p>Body</p>")
                        .attachments(List.of())
                        .build();

        NotificationRequestDTO request =
                NotificationRequestDTO.builder()
                        .channel(NotificationChannel.EMAIL)
                        .to("user@example.com")
                        .templateId("invite")
                        .locale(Locale.US)
                        .variables(Map.of("name", "User"))
                        .attachments(List.of(new AttachmentDTO("attach.txt", "text/plain", new byte[0], null, false, null)))
                        .build();

        String result = sender.send(request);

        assertThat(result).isEqualTo("sent-id");
        assertThat(emailService.to).isEqualTo("user@example.com");
        assertThat(emailService.subject).isEqualTo("Subject");
        assertThat(emailService.html).isEqualTo("<p>Body</p>");
        assertThat(emailService.attachments).hasSize(1);
        assertThat(templateService.lastLocale).isEqualTo(Locale.US);
    }

    @Test
    void send_whenTemplateAddsAttachments_mergesWithRequest() {
        templateService.rendered =
                RenderedTemplateDTO.builder()
                        .subject("Welcome")
                        .bodyHtml("<p>Body</p>")
                        .attachments(
                                List.of(
                                        new AttachmentDTO(
                                                "logo.png",
                                                "image/png",
                                                new byte[0],
                                                null,
                                                true,
                                                "logo")))
                        .build();

        NotificationRequestDTO request =
                NotificationRequestDTO.builder()
                        .channel(NotificationChannel.EMAIL)
                        .to("user@example.com")
                        .templateId("invite")
                        .attachments(
                                List.of(
                                        new AttachmentDTO(
                                                "qr.png", "image/png", new byte[0], null, true, "qr")))
                        .build();

        sender.send(request);

        assertThat(emailService.attachments).hasSize(2);
    }

    @Test
    void send_whenLocaleMissing_defaultsToEnglish() {
        templateService.rendered =
                RenderedTemplateDTO.builder().subject("Subject").bodyHtml("Body").attachments(List.of()).build();

        NotificationRequestDTO request =
                NotificationRequestDTO.builder()
                        .channel(NotificationChannel.EMAIL)
                        .to("user@example.com")
                        .templateId("invite")
                        .variables(Map.of())
                        .build();

        sender.send(request);

        assertThat(templateService.lastLocale).isEqualTo(Locale.ENGLISH);
    }

    private static final class RecordingTemplateService implements TemplateService {
        RenderedTemplateDTO rendered;
        Locale lastLocale;

        @Override
        public RenderedTemplateDTO render(String templateId, Map<String, Object> variables, Locale locale) {
            lastLocale = locale;
            return rendered;
        }
    }

    private static final class RecordingEmailService implements EmailService {
        String to;
        String subject;
        String html;
        List<AttachmentDTO> attachments;

        @Override
        public String send(String to, String subject, String html, List<AttachmentDTO> attachments) {
            this.to = to;
            this.subject = subject;
            this.html = html;
            this.attachments = attachments;
            return "sent-id";
        }
    }
}
