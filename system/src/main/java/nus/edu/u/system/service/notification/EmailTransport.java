package nus.edu.u.system.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.exception.RateLimitExceededException;
import nus.edu.u.framework.notification.email.EmailLimitPropertiesConfig;
import nus.edu.u.framework.notification.idempotency.IdempotencyKeyUtil;
import nus.edu.u.system.domain.dto.AttachmentDTO;
import nus.edu.u.system.domain.dto.EmailRequestDTO;
import nus.edu.u.system.domain.dto.NotificationRequestDTO;
import nus.edu.u.system.provider.email.EmailClient;
import nus.edu.u.system.provider.email.EmailClientFactory;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service("emailTransport")
@RequiredArgsConstructor
@Slf4j
public class EmailTransport implements TransportImplementor {

    private final EmailClientFactory emailClientFactory = EmailClientFactory.getInstance();
    private final IdempotencyService idempotency;
    private final RateLimiter rateLimiter;
    private final EmailLimitPropertiesConfig props;
    private final IdempotencyKeyUtil idempotencyKeys;

    public void process (NotificationRequestDTO notificationRequestDTO)
    {
        var to = notificationRequestDTO.getTo();
        var subject = notificationRequestDTO.getSubject();
        var html = notificationRequestDTO.getBody();
        var rateKey = props.getRateKey();
        var rateLimit = props.getRateLimit();
        var rateWindow = props.getRateWindow();
        var idempotencyTtl = props.getIdempotencyTtl();
        List<AttachmentDTO> attachments =
                (notificationRequestDTO.getAttachment() != null)
                        ? notificationRequestDTO.getAttachment()
                        : Collections.emptyList();

        final String requestId = UUID.randomUUID().toString();

        //rate limit
        if (!rateLimiter.allow(rateKey, rateLimit, rateWindow)) {
            throw new RateLimitExceededException("Rate limit exceeded for sending emails");
        }

        //idempotency
        final String idemKey = idempotencyKeys.buildEmailKey(to, subject, html);
        if (!idempotency.tryClaim(idemKey, idempotencyTtl)) {
            log.info("Email idempotency hit for key={} (requestId={})", idemKey, requestId);
        }

        try {
            EmailClient client = emailClientFactory.getClient(notificationRequestDTO.getEmailProvider());
            EmailRequestDTO requestDTO = EmailRequestDTO.builder()
                                                        .to(to)
                                                        .subject(subject)
                                                        .html(html)
                                                        .attachments(attachments)
                                                        .provider(notificationRequestDTO.getEmailProvider())
                                                        .build();
            client.sendEmail(requestDTO);
        }
        catch(Exception ex) {
            log.warn(
                    "Email send failed (requestId={}): to={}, subject={}, error={}",
                    requestId,
                    to,
                    subject,
                    ex.getMessage(),
                    ex);
            throw ex;
        }
    }
}
