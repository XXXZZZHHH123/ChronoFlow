package nus.edu.u.system.service.email;

import lombok.RequiredArgsConstructor;
import nus.edu.u.system.domain.dto.AttachmentDTO;
import nus.edu.u.system.domain.dto.NotificationRequestDTO;
import nus.edu.u.system.domain.vo.attendee.AttendeeInviteReqVO;
import nus.edu.u.system.enums.email.NotificationChannel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AttendeeEmailServiceImpl implements AttendeeEmailService {

    private static final String ATTENDEE_INVITE_TEMPLATE_ID = "attendee-qr-invite";
    private static final String LOGO_CID = "logo";

    private final NotificationService notificationService;

    @Override
    public String sendAttendeeInvite(AttendeeInviteReqVO req) {

        Map<String, Object> vars = getAttendeeInviteTemplateVars(req);

        List<AttachmentDTO> attachments = new ArrayList<>();
        attachments.addAll(getQrAttachment(req));
        attachments.addAll(getInlineLogoAttachment());

        var request = NotificationRequestDTO.builder()
                .channel(NotificationChannel.EMAIL)
                .to(req.getToEmail())
                .templateId(ATTENDEE_INVITE_TEMPLATE_ID)
                .variables(vars)
                .locale(Locale.ENGLISH)
                .attachments(attachments)
                .build();

        return notificationService.send(request);
    }

    private static Map<String, Object> getAttendeeInviteTemplateVars(AttendeeInviteReqVO req) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("subject", "Your QR code for " + req.getOrganizationName());
        vars.put("attendeeName", req.getAttendeeName());
        vars.put("organizationName", req.getOrganizationName());
        vars.put("logoCid", LOGO_CID);
        return vars;
    }

    private static List<AttachmentDTO> getQrAttachment(AttendeeInviteReqVO req) {
        List<AttachmentDTO> attachments = new ArrayList<>();
        if (req.getQrFile() != null && !req.getQrFile().isEmpty()) {
            try {
                attachments.add(new AttachmentDTO(
                        req.getQrFile().getOriginalFilename(),
                        req.getQrFile().getContentType() != null ? req.getQrFile().getContentType() : "image/png",
                        req.getQrFile().getBytes(),
                        null,
                        false,
                        null
                ));
            } catch (Exception e) {
                throw new RuntimeException("Failed to read QR code attachment", e);
            }
        }
        return attachments;
    }

    private static List<AttachmentDTO> getInlineLogoAttachment() {
        List<AttachmentDTO> attachments = new ArrayList<>();
        try {
            var res = new ClassPathResource("images/email/logo.png");
            if (res.exists()) {
                byte[] bytes = StreamUtils.copyToByteArray(res.getInputStream());
                attachments.add(new AttachmentDTO(
                        "logo.png",
                        "image/png",
                        bytes,
                        null,
                        true,
                        LOGO_CID
                ));
            }
        } catch (Exception ignored) {
            // ignore missing logo
        }
        return attachments;
    }
}