package nus.edu.u.system.service.email;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import nus.edu.u.system.domain.dto.AttachmentDTO;
import nus.edu.u.system.domain.dto.NotificationRequestDTO;
import nus.edu.u.system.enums.email.NotificationChannel;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationSender {

    private final TemplateService templateService;
    private final EmailService emailService;

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL;
    }

    @Override
    public String send(NotificationRequestDTO request) {

        var locale = request.locale() != null ? request.locale() : Locale.ENGLISH;
        var rendered = templateService.render(request.templateId(), request.variables(), locale);

        // Merge attachments (template + request)
        List<AttachmentDTO> attachments = new ArrayList<>();
        if (rendered.attachments() != null) attachments.addAll(rendered.attachments());
        if (request.attachments() != null) attachments.addAll(request.attachments());

        return emailService.send(
                request.to(), rendered.subject(), rendered.bodyHtml(), attachments);
    }
}
