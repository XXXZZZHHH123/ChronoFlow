package nus.edu.u.system.service.email;

import lombok.RequiredArgsConstructor;
import nus.edu.u.system.domain.dto.AttachmentDTO;
import nus.edu.u.system.domain.dto.NotificationRequestDTO;
import nus.edu.u.system.domain.vo.reg.RegSearchReqVO;
import nus.edu.u.system.enums.email.NotificationChannel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MemberEmailServiceImpl implements MemberEmailService {

    private static final String MEMBER_INVITE_TEMPLATE_ID = "member-invite";
    private static final String LOGO_CID = "logo";
    private static final String INVITE_BASE_URL = "https://chronoflow-frontend-production.up.railway.app/login";

    private final NotificationService notificationService;

    @Override
    public String sendMemberInviteEmail(String recipientEmail, RegSearchReqVO req) {

        String inviteUrl = INVITE_BASE_URL +
                "?organisation_id=" + req.getOrganizationId() +
                "&user_id=" + req.getUserId();

        Map<String, Object> vars = getMemberInviteVars(req, inviteUrl);

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

        }

        var request = NotificationRequestDTO.builder()
                .channel(NotificationChannel.EMAIL)
                .to(recipientEmail)
                .templateId(MEMBER_INVITE_TEMPLATE_ID)
                .variables(vars)
                .locale(Locale.ENGLISH)
                .attachments(attachments)
                .build();

        return notificationService.send(request);
    }

    private static Map<String, Object> getMemberInviteVars(RegSearchReqVO req, String inviteUrl) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("subject", "You’re invited to join ChronoFlow");
        vars.put("organizationId", req.getOrganizationId());
        vars.put("userId", req.getUserId());
        vars.put("inviteUrl", inviteUrl);
        vars.put("logoCid", LOGO_CID);
        return vars;
    }
}