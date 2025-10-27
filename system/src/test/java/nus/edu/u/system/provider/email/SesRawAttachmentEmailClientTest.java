package nus.edu.u.system.provider.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import nus.edu.u.framework.notification.email.EmailProviderPropertiesConfig;
import nus.edu.u.system.domain.dto.AttachmentDTO;
import nus.edu.u.system.domain.dto.EmailSendResultDTO;
import nus.edu.u.system.enums.email.EmailProvider;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;

class SesRawAttachmentEmailClientTest {

    @Test
    void sendEmail_withInlineAndRegularAttachments_buildsRawMessage() {
        RecordingSesClient sesClient = new RecordingSesClient();
        SesRawAttachmentEmailClient client =
                new SesRawAttachmentEmailClient(sesClient, props("sender@example.com"));

        AttachmentDTO inlineImage =
                new AttachmentDTO(
                        "logo.png",
                        "image/png",
                        "img".getBytes(StandardCharsets.UTF_8),
                        null,
                        true,
                        "logo");
        AttachmentDTO document =
                new AttachmentDTO(
                        "agenda.pdf",
                        "application/pdf",
                        "pdf".getBytes(StandardCharsets.UTF_8),
                        null,
                        false,
                        null);

        EmailSendResultDTO result =
                client.sendEmail(
                        "user@example.com",
                        "Welcome",
                        "<p>Hello</p>",
                        List.of(inlineImage, document));

        assertThat(result.provider()).isEqualTo(EmailProvider.AWS_SES);
        assertThat(result.providerMessageId()).isEqualTo("raw-message");

        SendEmailRequest request = sesClient.lastRequest;
        byte[] rawBytes = request.content().raw().data().asByteArray();
        String rawText = new String(rawBytes, StandardCharsets.UTF_8);
        assertThat(rawText).contains("Content-Disposition: inline");
        assertThat(rawText).contains("Content-Disposition: attachment");
        assertThat(rawText).contains("logo.png");
        assertThat(rawText).contains("agenda.pdf");
    }

    @Test
    void sendEmail_missingContentTypeFallsBackToOctetStream() {
        RecordingSesClient sesClient = new RecordingSesClient();
        SesRawAttachmentEmailClient client =
                new SesRawAttachmentEmailClient(sesClient, props("sender@example.com"));

        AttachmentDTO attachment =
                new AttachmentDTO(
                        "file.bin",
                        null,
                        "payload".getBytes(StandardCharsets.UTF_8),
                        null,
                        false,
                        null);

        client.sendEmail("user@example.com", "Fallback", "<p>Hi</p>", List.of(attachment));

        String rawText =
                new String(
                        sesClient.lastRequest.content().raw().data().asByteArray(),
                        StandardCharsets.UTF_8);
        assertThat(rawText).contains("application/octet-stream");
    }

    private static EmailProviderPropertiesConfig props(String from) {
        EmailProviderPropertiesConfig config = new EmailProviderPropertiesConfig();
        config.setFrom(from);
        return config;
    }

    private static final class RecordingSesClient implements SesV2Client {
        SendEmailRequest lastRequest;

        @Override
        public SendEmailResponse sendEmail(SendEmailRequest request) {
            this.lastRequest = request;
            return SendEmailResponse.builder().messageId("raw-message").build();
        }

        @Override
        public String serviceName() {
            return "ses";
        }

        @Override
        public void close() {}
    }
}
