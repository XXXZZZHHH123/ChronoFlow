package nus.edu.u.system.provider;

import lombok.RequiredArgsConstructor;
import nus.edu.u.system.domain.dto.AttachmentDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EmailClientFactory {
    private final SesEmailClient sesEmailClient;
    private final SesRawAttachmentEmailClient sesRawAttachmentEmailClient;

    public EmailClient getClient(List<AttachmentDTO> attachments) {
        boolean hasAttachments = attachments != null && !attachments.isEmpty();
        return hasAttachments ? sesRawAttachmentEmailClient : sesEmailClient;
    }
}