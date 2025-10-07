package nus.edu.u.system.provider;

import java.util.List;
import lombok.RequiredArgsConstructor;
import nus.edu.u.system.domain.dto.AttachmentDTO;
import org.springframework.stereotype.Component;

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
