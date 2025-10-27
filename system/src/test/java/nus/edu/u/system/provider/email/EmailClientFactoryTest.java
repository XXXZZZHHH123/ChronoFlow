package nus.edu.u.system.provider.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nus.edu.u.framework.notification.email.EmailProviderPropertiesConfig;
import nus.edu.u.system.domain.dto.AttachmentDTO;
import org.junit.jupiter.api.Test;

class EmailClientFactoryTest {

    @Test
    void getClient_withoutAttachments_returnsSimpleClient() {
        SesEmailClient simple =
                new SesEmailClient(new RecordingSesClient(), props("simple@example.com"));
        SesRawAttachmentEmailClient raw =
                new SesRawAttachmentEmailClient(new RecordingSesClient(), props("raw@example.com"));
        EmailClientFactory factory = new EmailClientFactory(simple, raw);

        assertThat(factory.getClient(null)).isSameAs(simple);
        assertThat(factory.getClient(List.of())).isSameAs(simple);
    }

    @Test
    void getClient_withAttachments_returnsRawClient() {
        SesEmailClient simple =
                new SesEmailClient(new RecordingSesClient(), props("simple@example.com"));
        SesRawAttachmentEmailClient raw =
                new SesRawAttachmentEmailClient(new RecordingSesClient(), props("raw@example.com"));
        EmailClientFactory factory = new EmailClientFactory(simple, raw);

        AttachmentDTO attachment =
                new AttachmentDTO(
                        "agenda.pdf", "application/pdf", new byte[] {1}, null, false, null);

        assertThat(factory.getClient(List.of(attachment))).isSameAs(raw);
    }

    private static EmailProviderPropertiesConfig props(String from) {
        EmailProviderPropertiesConfig config = new EmailProviderPropertiesConfig();
        config.setFrom(from);
        return config;
    }

    private static final class RecordingSesClient
            implements software.amazon.awssdk.services.sesv2.SesV2Client {
        @Override
        public software.amazon.awssdk.services.sesv2.model.SendEmailResponse sendEmail(
                software.amazon.awssdk.services.sesv2.model.SendEmailRequest request) {
            return software.amazon.awssdk.services.sesv2.model.SendEmailResponse.builder()
                    .messageId("factory-test")
                    .build();
        }

        @Override
        public String serviceName() {
            return "ses";
        }

        @Override
        public void close() {}
    }
}
