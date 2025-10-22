package nus.edu.u.system.service.email;

import java.util.List;
import nus.edu.u.system.domain.dto.AttachmentDTO;

public interface EmailService {
    String send(String to, String subject, String html, List<AttachmentDTO> attachments);
}
