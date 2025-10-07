package nus.edu.u.system.service.email;


import lombok.RequiredArgsConstructor;
import nus.edu.u.system.domain.dto.AttachmentDTO;
import nus.edu.u.system.domain.dto.NotificationRequestDTO;
import nus.edu.u.system.domain.vo.reg.RegOrganizerReqVO;
import nus.edu.u.system.enums.email.NotificationChannel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
public class OrganizerEmailServiceImpl implements OrganizerEmailService {

    private static final String WELCOME_EMAIL_ORGANIZER_TEMPLATE_ID = "welcome-email-organizer";
    private static final String LOGO_CID = "logo";

    private final NotificationService notificationService;

    @Override
    public String sendWelcomeEmailOrganizer(RegOrganizerReqVO req) {

        Map<String, Object> vars = getOrganizerRequestVars(req);

        List<AttachmentDTO> attachments = new ArrayList<>();
        try {
            var res = new ClassPathResource("images/email/logo.png");
            if (res.exists()) {
                byte[] bytes = StreamUtils.copyToByteArray(res.getInputStream());
                attachments.add(new AttachmentDTO(
                        "logo.png",
                        "image/png",
                        bytes,
                        null,   // url
                        true,   // inline
                        LOGO_CID
                ));
            }
        } catch (Exception ignored) {
            // if logo missing, just proceed without it
        }

        var request = NotificationRequestDTO.builder()
                .channel(NotificationChannel.EMAIL)
                .to(req.getUserEmail())
                .templateId(WELCOME_EMAIL_ORGANIZER_TEMPLATE_ID)
                .variables(vars)
                .locale(Locale.ENGLISH)
                .attachments(attachments)
                .build();

        return notificationService.send(request);
    }

    private static Map<String, Object> getOrganizerRequestVars(RegOrganizerReqVO req) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("subject", "Welcome to ChronoFlow, " + req.getName() + "!");
        vars.put("name", req.getName());
        vars.put("username", req.getUsername());
        vars.put("email", req.getUserEmail());
        vars.put("mobile", req.getMobile());
        vars.put("organizationName", req.getOrganizationName());
        vars.put("organizationAddress", req.getOrganizationAddress() == null ? "" : req.getOrganizationAddress());
        vars.put("organizationCode", req.getOrganizationCode() == null ? "" : req.getOrganizationCode());
        vars.put("logoCid", LOGO_CID);
        return vars;
    }
}