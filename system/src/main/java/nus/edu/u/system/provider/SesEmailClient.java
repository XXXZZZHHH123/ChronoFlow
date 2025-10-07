package nus.edu.u.system.provider;

import lombok.RequiredArgsConstructor;
import nus.edu.u.framework.notification.email.EmailProviderPropertiesConfig;
import nus.edu.u.system.domain.dto.AttachmentDTO;
import nus.edu.u.system.domain.dto.EmailSendResultDTO;
import nus.edu.u.system.enums.email.EmailProvider;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SesEmailClient implements EmailClient {

    private final SesV2Client ses;
    private final EmailProviderPropertiesConfig props;

    @Override
    public EmailSendResultDTO sendEmail(String to, String subject, String html, List<AttachmentDTO> attachments) {
        if (attachments != null && !attachments.isEmpty())
            throw new UnsupportedOperationException("Use raw client for attachments");

        var req = SendEmailRequest.builder()
                .fromEmailAddress(props.getFrom())
                .destination(Destination.builder().toAddresses(to).build())
                .content(EmailContent.builder()
                        .simple(Message.builder()
                                .subject(Content.builder().data(subject).build())
                                .body(Body.builder().html(Content.builder().data(html).build()).build())
                                .build())
                        .build())
                .build();

        var resp = ses.sendEmail(req);
        return new EmailSendResultDTO(EmailProvider.AWS_SES, resp.messageId());
    }
}