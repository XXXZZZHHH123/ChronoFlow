package nus.edu.u.system.service.email;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.exception.RateLimitExceededException;
import nus.edu.u.framework.notification.email.EmailLimitPropertiesConfig;
import nus.edu.u.system.domain.dto.AttachmentDTO;
import nus.edu.u.system.domain.dto.EmailSendResultDTO;
import nus.edu.u.system.provider.email.EmailClient;
import nus.edu.u.system.provider.email.EmailClientFactory;
import nus.edu.u.system.util.IdempotencyKeyUtil;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final IdempotencyService idempotency;
    private final RateLimiter rateLimiter;
    private final EmailLimitPropertiesConfig props;
    private final IdempotencyKeyUtil idempotencyKeys;
    private final EmailClientFactory emailClientFactory;

    @Override
    public String send(String to, String subject, String html, List<AttachmentDTO> attachments) {

        final String requestId = UUID.randomUUID().toString();

        // 1) Rate limit
        if (!rateLimiter.allow(props.getRateKey(), props.getRateLimit(), props.getRateWindow())) {
            throw new RateLimitExceededException("Rate limit exceeded for sending emails");
        }

        // 2) Idempotency
        final String idemKey = idempotencyKeys.buildEmailKey(to, subject, html);
        if (!idempotency.tryClaim(idemKey, props.getIdempotencyTtl())) {
            log.info("Email idempotency hit for key={} (requestId={})", idemKey, requestId);
            return "ALREADY_ACCEPTED";
        }

        // 3) Choose provider and send
        try {
            List<AttachmentDTO> safe =
                    (attachments == null) ? Collections.emptyList() : attachments;
            EmailClient client = emailClientFactory.getClient(safe);

            EmailSendResultDTO result = client.sendEmail(to, subject, html, safe);
            String provider = result.provider() != null ? result.provider().name() : "UNKNOWN";
            String providerMsgId = result.providerMessageId();

            log.info(
                    "Email sent (requestId={}): provider={}, providerMsgId={}, to={}, subject={}",
                    requestId,
                    provider,
                    providerMsgId,
                    to,
                    subject);

            // Prefer returning provider message id if present; else return our requestId
            return (providerMsgId != null && !providerMsgId.isBlank()) ? providerMsgId : requestId;

        } catch (Exception ex) {
            log.warn(
                    "Email send failed (requestId={}): to={}, subject={}, error={}",
                    requestId,
                    to,
                    subject,
                    ex.getMessage(),
                    ex);
            // Surface the failure to the caller (you could also rethrow or map to a domain error)
            throw ex;
        }
    }
}
