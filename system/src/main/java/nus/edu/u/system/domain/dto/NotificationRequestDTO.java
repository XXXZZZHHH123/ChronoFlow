package nus.edu.u.system.domain.dto;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.Builder;
import nus.edu.u.system.enums.email.NotificationChannel;

@Builder
public record NotificationRequestDTO(
        NotificationChannel channel,
        String to,
        String templateId,
        Map<String, Object> variables,
        Locale locale,
        List<AttachmentDTO> attachments) {}
