package nus.edu.u.system.provider;

import nus.edu.u.system.domain.dto.AttachmentDTO;
import nus.edu.u.system.domain.dto.EmailSendResultDTO;

import java.util.List;

public interface EmailClient {
    EmailSendResultDTO sendEmail(String to, String subject, String html, List<AttachmentDTO> attachments);
}