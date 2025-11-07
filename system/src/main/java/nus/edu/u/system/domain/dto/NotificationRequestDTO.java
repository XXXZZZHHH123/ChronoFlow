package nus.edu.u.system.domain.dto;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.*;
import nus.edu.u.system.enums.email.EmailProvider;
import nus.edu.u.system.enums.email.NotificationChannel;
import nus.edu.u.system.enums.email.TemplateProvider;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationRequestDTO {
    NotificationChannel channel;
    EmailProvider emailProvider;
    String to;
    String templateId;
    String subject;
    String body;
    TemplateProvider templateProvider;
    Map<String, Object> variables;
    Locale locale;
    List<AttachmentDTO> attachment;
}
