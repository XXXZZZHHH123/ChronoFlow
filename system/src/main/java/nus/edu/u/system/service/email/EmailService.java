package nus.edu.u.system.service.email;

import nus.edu.u.system.domain.dto.AttachmentDTO;

import java.util.List;

public interface EmailService {
    String send(String to, String subject, String html, List<AttachmentDTO> attachments);
}