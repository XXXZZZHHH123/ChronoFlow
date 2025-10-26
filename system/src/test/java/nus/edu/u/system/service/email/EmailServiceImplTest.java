package nus.edu.u.system.service.email;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.util.List;
import nus.edu.u.common.exception.RateLimitExceededException;
import nus.edu.u.framework.notification.email.EmailLimitPropertiesConfig;
import nus.edu.u.framework.notification.email.EmailProviderPropertiesConfig;
import nus.edu.u.framework.notification.idempotency.IdempotencyKeyUtil;
import nus.edu.u.framework.notification.idempotency.IdempotencyPropertiesConfig;
import nus.edu.u.system.provider.email.EmailClientFactory;
import nus.edu.u.system.provider.email.SesEmailClient;
import nus.edu.u.system.provider.email.SesRawAttachmentEmailClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;

class EmailServiceImplTest {

    private EmailServiceImpl emailService;
    private RecordingRateLimiter rateLimiter;
    private RecordingIdempotencyService idempotencyService;
    private EmailLimitPropertiesConfig props;
    private RecordingSesClient simpleSesClient;
    private EmailClientFactory clientFactory;

    @BeforeEach
    void setUp() {
        rateLimiter = new RecordingRateLimiter();
        idempotencyService = new RecordingIdempotencyService();
        props = new EmailLimitPropertiesConfig();
        props.setRateLimit(5);
        props.setRateWindow(Duration.ofSeconds(10));
        props.setIdempotencyTtl(Duration.ofMinutes(5));
        props.setRateKey("email-rate");

        IdempotencyPropertiesConfig idemProps = new IdempotencyPropertiesConfig();
        idemProps.setEmailPrefix("email:");
        idemProps.setPushPrefix("push:");
        IdempotencyKeyUtil keyUtil = new IdempotencyKeyUtil(idemProps);

        simpleSesClient = new RecordingSesClient("provider-msg");
        RecordingSesClient rawSesClient = new RecordingSesClient("provider-raw");
        clientFactory =
                new EmailClientFactory(
                        new SesEmailClient(simpleSesClient, emailProps("simple@example.com")),
                        new SesRawAttachmentEmailClient(
                                rawSesClient, emailProps("raw@example.com")));

        emailService =
                new EmailServiceImpl(
                        idempotencyService, rateLimiter, props, keyUtil, clientFactory);
    }

    @Test
    void send_successfulEmailReturnsProviderMessageId() {
        String messageId =
                emailService.send("user@example.com", "Subject", "<p>Hello</p>", List.of());

        assertThat(messageId).isNotBlank();
        assertThat(rateLimiter.wasCalled).isTrue();
        assertThat(idempotencyService.lastClaimedKey).contains("email:user@example.com");
    }

    @Test
    void send_rateLimitExceededThrows() {
        rateLimiter.allow = false;

        assertThatThrownBy(
                        () ->
                                emailService.send(
                                        "user@example.com", "Subject", "<p>Hello</p>", List.of()))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void send_idempotencyHitReturnsAlreadyAccepted() {
        idempotencyService.claimResult = false;

        String result = emailService.send("user@example.com", "Subject", "<p>Hello</p>", List.of());

        assertThat(result).isEqualTo("ALREADY_ACCEPTED");
    }

    @Test
    void send_whenProviderReturnsEmptyMessageId_fallsBackToRequestId() {
        simpleSesClient.messageId = "";

        String result = emailService.send("user@example.com", "Subject", "<p>Hello</p>", List.of());

        assertThat(result).hasSizeGreaterThan(0);
        assertThat(result).doesNotContain("ALREADY_ACCEPTED");
    }

    private static final class RecordingRateLimiter implements RateLimiter {
        boolean allow = true;
        boolean wasCalled;

        @Override
        public boolean allow(String key, int limit, Duration window) {
            wasCalled = true;
            return allow;
        }
    }

    private static final class RecordingIdempotencyService implements IdempotencyService {
        boolean claimResult = true;
        String lastClaimedKey;

        @Override
        public boolean tryClaim(String key, Duration ttl) {
            lastClaimedKey = key;
            return claimResult;
        }
    }

    private static EmailProviderPropertiesConfig emailProps(String from) {
        EmailProviderPropertiesConfig config = new EmailProviderPropertiesConfig();
        config.setFrom(from);
        return config;
    }

    private static final class RecordingSesClient implements SesV2Client {
        String messageId;

        private RecordingSesClient(String messageId) {
            this.messageId = messageId;
        }

        @Override
        public SendEmailResponse sendEmail(SendEmailRequest request) {
            return SendEmailResponse.builder().messageId(messageId).build();
        }

        @Override
        public String serviceName() {
            return "ses";
        }

        @Override
        public void close() {}
    }
}
