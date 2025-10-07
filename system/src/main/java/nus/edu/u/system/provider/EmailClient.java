package nus.edu.u.system.provider;

import java.util.List;
import nus.edu.u.system.domain.dto.AttachmentDTO;
import nus.edu.u.system.domain.dto.EmailSendResultDTO;

public interface EmailClient {
    EmailSendResultDTO sendEmail(
            String to, String subject, String html, List<AttachmentDTO> attachments);
}
