package nus.edu.u.system.provider.email;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import nus.edu.u.framework.notification.email.EmailProviderPropertiesConfig;
import nus.edu.u.system.domain.dto.AttachmentDTO;
import nus.edu.u.system.domain.dto.EmailSendResultDTO;
import nus.edu.u.system.enums.email.EmailProvider;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;

class SesEmailClientTest {

    @Test
    void sendEmail_withoutAttachments_invokesSes() {
        StubSesClient sesClient = new StubSesClient();
        EmailProviderPropertiesConfig props = new EmailProviderPropertiesConfig();
        props.setFrom("noreply@example.com");
        SesEmailClient client = new SesEmailClient(sesClient, props);

        EmailSendResultDTO result =
                client.sendEmail("user@example.com", "Subject", "<p>Hi</p>", null);

        assertThat(result.provider()).isEqualTo(EmailProvider.AWS_SES);
        assertThat(result.providerMessageId()).isEqualTo("message-id");

        SendEmailRequest sent = sesClient.lastRequest;
        assertThat(sent.fromEmailAddress()).isEqualTo("noreply@example.com");
        assertThat(sent.destination())
                .isEqualTo(Destination.builder().toAddresses("user@example.com").build());
        assertThat(sent.content().simple().subject().data()).isEqualTo("Subject");
    }

    @Test
    void sendEmail_withAttachments_throws() {
        SesEmailClient client = new SesEmailClient(new StubSesClient(), props("from@example.com"));
        AttachmentDTO attachment =
                new AttachmentDTO("a.txt", "text/plain", new byte[] {1}, null, false, null);

        assertThatThrownBy(() -> client.sendEmail("to", "s", "h", List.of(attachment)))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Use raw client");
    }

    private static EmailProviderPropertiesConfig props(String from) {
        EmailProviderPropertiesConfig props = new EmailProviderPropertiesConfig();
        props.setFrom(from);
        return props;
    }

    private static final class StubSesClient implements SesV2Client {
        SendEmailRequest lastRequest;

        @Override
        public SendEmailResponse sendEmail(SendEmailRequest request) {
            this.lastRequest = request;
            return SendEmailResponse.builder().messageId("message-id").build();
        }

        @Override
        public String serviceName() {
            return "ses";
        }

        @Override
        public void close() {}
    }
}
