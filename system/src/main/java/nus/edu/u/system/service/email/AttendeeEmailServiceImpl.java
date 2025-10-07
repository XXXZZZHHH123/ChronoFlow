package nus.edu.u.system.service.email;

import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.system.domain.dataobject.attendee.EventAttendeeDO;
import nus.edu.u.system.domain.dataobject.task.EventDO;
import nus.edu.u.system.domain.dto.AttachmentDTO;
import nus.edu.u.system.domain.dto.NotificationRequestDTO;
import nus.edu.u.system.domain.vo.attendee.AttendeeInviteReqVO;
import nus.edu.u.system.domain.vo.qrcode.QrCodeRespVO;
import nus.edu.u.system.enums.email.NotificationChannel;
import nus.edu.u.system.mapper.attendee.EventAttendeeMapper;
import nus.edu.u.system.mapper.task.EventMapper;
import nus.edu.u.system.service.qrcode.QrCodeService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendeeEmailServiceImpl implements AttendeeEmailService {

    private static final String ATTENDEE_INVITE_TEMPLATE_ID = "attendee-qr-invite";
    private static final String LOGO_CID = "logo";
    private static final String QR_CODE_CID = "qrcode";
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NotificationService notificationService;
    private final QrCodeService qrCodeService;
    private final EventAttendeeMapper attendeeMapper;
    private final EventMapper eventMapper;

    @Override
    @Transactional
    public String sendAttendeeInvite(AttendeeInviteReqVO req) {
        log.info("Sending attendee invite to {} for event {}", req.getToEmail(), req.getEventId());

        if (req.getToEmail() == null || req.getToEmail().isBlank()) {
            throw new IllegalArgumentException("Attendee email is required");
        }
        if (req.getAttendeeName() == null || req.getAttendeeName().isBlank()) {
            throw new IllegalArgumentException("Attendee name is required");
        }

        EventDO event = null;
        if (req.getEventId() != null) {
            event = eventMapper.selectById(req.getEventId());
            if (event == null) {
                throw exception(EVENT_NOT_FOUND);
            }
        }

        Map<String, Object> vars = getAttendeeInviteTemplateVars(req, event);
        List<AttachmentDTO> attachments = new ArrayList<>();

        if (req.getEventId() != null) {
            attachments.addAll(getGeneratedQrAttachment(req, event));
        } else if (req.getQrFile() != null && !req.getQrFile().isEmpty()) {
            attachments.addAll(getUploadedQrAttachment(req));
        } else {
            throw new IllegalArgumentException("Either eventId or qrFile must be provided");
        }

        attachments.addAll(getInlineLogoAttachment());

        var request =
                NotificationRequestDTO.builder()
                        .channel(NotificationChannel.EMAIL)
                        .to(req.getToEmail())
                        .templateId(ATTENDEE_INVITE_TEMPLATE_ID)
                        .variables(vars)
                        .locale(Locale.ENGLISH)
                        .attachments(attachments)
                        .build();

        String messageId = notificationService.send(request);
        log.info("Attendee invite sent successfully. MessageId: {}", messageId);
        return messageId;
    }

    private Map<String, Object> getAttendeeInviteTemplateVars(
            AttendeeInviteReqVO req, EventDO event) {
        Map<String, Object> vars = new HashMap<>();

        vars.put("attendeeName", req.getAttendeeName());
        vars.put("organizationName", req.getOrganizationName());
        vars.put("logoCid", LOGO_CID);
        vars.put("qrCodeCid", QR_CODE_CID);

        if (event != null) {
            vars.put(
                    "subject",
                    "Your Event QR Code for "
                            + (event.getName() != null
                                    ? event.getName()
                                    : req.getOrganizationName()));
            vars.put("eventName", event.getName() != null ? event.getName() : "");

            // 格式化日期时间
            String eventDate = "";
            if (event.getStartTime() != null) {
                eventDate = event.getStartTime().format(DATE_FORMATTER);
                if (event.getEndTime() != null) {
                    eventDate += " - " + event.getEndTime().format(DATE_FORMATTER);
                }
            }
            vars.put("eventDate", eventDate);

            vars.put("eventLocation", event.getLocation() != null ? event.getLocation() : "");
            vars.put(
                    "eventDescription",
                    event.getDescription() != null ? event.getDescription() : "");
        } else {
            vars.put("subject", "Your Event QR Code for " + req.getOrganizationName());
            vars.put("eventName", req.getEventName() != null ? req.getEventName() : "");
            vars.put("eventDate", req.getEventDate() != null ? req.getEventDate() : "");
            vars.put("eventLocation", req.getEventLocation() != null ? req.getEventLocation() : "");
            vars.put(
                    "eventDescription",
                    req.getEventDescription() != null ? req.getEventDescription() : "");
        }

        return vars;
    }

    private List<AttachmentDTO> getGeneratedQrAttachment(AttendeeInviteReqVO req, EventDO event) {
        List<AttachmentDTO> attachments = new ArrayList<>();
        try {
            EventAttendeeDO attendee =
                    attendeeMapper.selectByEventAndEmail(req.getEventId(), req.getToEmail());

            String token;
            if (attendee == null) {
                token = UUID.randomUUID().toString();
                attendee =
                        EventAttendeeDO.builder()
                                .eventId(req.getEventId())
                                .attendeeEmail(req.getToEmail())
                                .attendeeName(req.getAttendeeName())
                                .attendeeMobile(req.getAttendeeMobile())
                                .checkInToken(token)
                                .checkInStatus(0)
                                .qrCodeGeneratedTime(LocalDateTime.now())
                                .build();

                attendeeMapper.insert(attendee);
                log.info(
                        "Created new attendee: email={}, eventId={}",
                        req.getToEmail(),
                        req.getEventId());
            } else {
                boolean needUpdate = false;

                if (!attendee.getAttendeeName().equals(req.getAttendeeName())) {
                    attendee.setAttendeeName(req.getAttendeeName());
                    needUpdate = true;
                }

                if (req.getAttendeeMobile() != null
                        && !req.getAttendeeMobile().equals(attendee.getAttendeeMobile())) {
                    attendee.setAttendeeMobile(req.getAttendeeMobile());
                    needUpdate = true;
                }

                if (attendee.getCheckInToken() == null) {
                    token = UUID.randomUUID().toString();
                    attendee.setCheckInToken(token);
                    attendee.setQrCodeGeneratedTime(LocalDateTime.now());
                    needUpdate = true;
                } else {
                    token = attendee.getCheckInToken();
                }

                if (needUpdate) {
                    attendeeMapper.updateById(attendee);
                    log.info("Updated attendee: email={}", req.getToEmail());
                } else {
                    log.info("Using existing attendee record: email={}", req.getToEmail());
                }
            }

            QrCodeRespVO qrCode = qrCodeService.generateEventCheckInQrWithToken(token);
            byte[] qrBytes = Base64.getDecoder().decode(qrCode.getBase64Image());

            attachments.add(
                    new AttachmentDTO(
                            "event-qrcode.png",
                            "image/png",
                            qrBytes,
                            null,
                            true, // inline
                            QR_CODE_CID));

            log.info(
                    "QR code generated successfully for email={}, token={}",
                    req.getToEmail(),
                    token.substring(0, 8) + "...");

        } catch (Exception e) {
            log.error("Failed to generate QR code for email={}", req.getToEmail(), e);
            throw new RuntimeException("Failed to generate QR code: " + e.getMessage(), e);
        }
        return attachments;
    }

    private List<AttachmentDTO> getUploadedQrAttachment(AttendeeInviteReqVO req) {
        List<AttachmentDTO> attachments = new ArrayList<>();
        try {
            attachments.add(
                    new AttachmentDTO(
                            req.getQrFile().getOriginalFilename(),
                            req.getQrFile().getContentType() != null
                                    ? req.getQrFile().getContentType()
                                    : "image/png",
                            req.getQrFile().getBytes(),
                            null,
                            true, // inline
                            QR_CODE_CID));
            log.info("Using uploaded QR code file");
        } catch (Exception e) {
            log.error("Failed to read uploaded QR file", e);
            throw new RuntimeException("Failed to read QR code: " + e.getMessage(), e);
        }
        return attachments;
    }

    private List<AttachmentDTO> getInlineLogoAttachment() {
        List<AttachmentDTO> attachments = new ArrayList<>();
        try {
            var res = new ClassPathResource("images/email/logo.png");
            if (res.exists()) {
                byte[] bytes = StreamUtils.copyToByteArray(res.getInputStream());
                attachments.add(
                        new AttachmentDTO("logo.png", "image/png", bytes, null, true, LOGO_CID));
            }
        } catch (Exception e) {
            log.warn("Logo not found, continuing without it", e);
        }
        return attachments;
    }
}
