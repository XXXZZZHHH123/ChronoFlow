package nus.edu.u.system.service.attendee;

import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.framework.mybatis.MybatisPlusConfig.getCurrentTenantId;
import static nus.edu.u.system.enums.ErrorCodeConstants.*;

import cn.hutool.core.util.ObjectUtil;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.system.domain.dataobject.attendee.EventAttendeeDO;
import nus.edu.u.system.domain.dataobject.task.EventDO;
import nus.edu.u.system.domain.dataobject.tenant.TenantDO;
import nus.edu.u.system.domain.vo.attendee.AttendeeInfoRespVO;
import nus.edu.u.system.domain.vo.attendee.AttendeeInviteReqVO;
import nus.edu.u.system.domain.vo.attendee.AttendeeQrCodeRespVO;
import nus.edu.u.system.domain.vo.attendee.AttendeeReqVO;
import nus.edu.u.system.domain.vo.checkin.CheckInRespVO;
import nus.edu.u.system.domain.vo.checkin.GenerateQrCodesReqVO;
import nus.edu.u.system.domain.vo.checkin.GenerateQrCodesRespVO;
import nus.edu.u.system.domain.vo.qrcode.QrCodeRespVO;
import nus.edu.u.system.mapper.attendee.EventAttendeeMapper;
import nus.edu.u.system.mapper.task.EventMapper;
import nus.edu.u.system.mapper.tenant.TenantMapper;
import nus.edu.u.system.service.attendee.validation.CheckInValidationChainBuilder;
import nus.edu.u.system.service.attendee.validation.CheckInValidationContext;
import nus.edu.u.system.service.attendee.validation.CheckInValidator;
import nus.edu.u.system.service.notification.AttendeeEmailService;
import nus.edu.u.system.service.qrcode.QrCodeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class AttendeeServiceImpl implements AttendeeService {

    @Resource private EventAttendeeMapper attendeeMapper;
    @Resource private EventMapper eventMapper;
    @Resource private QrCodeService qrCodeService;
    @Resource private AttendeeEmailService attendeeEmailService;
    @Resource private TenantMapper tenantMapper;
    @Resource private CheckInValidationChainBuilder validationChainBuilder;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    public List<AttendeeQrCodeRespVO> list(Long eventId) {
        List<EventAttendeeDO> list = attendeeMapper.selectByEventId(eventId);
        if (ObjectUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream()
                .map(
                        attendee ->
                                AttendeeQrCodeRespVO.builder()
                                        .id(attendee.getId())
                                        .attendeeEmail(attendee.getAttendeeEmail())
                                        .attendeeMobile(attendee.getAttendeeMobile())
                                        .attendeeName(attendee.getAttendeeName())
                                        .checkInToken(attendee.getCheckInToken())
                                        .checkInStatus(attendee.getCheckInStatus())
                                        .build())
                .toList();
    }

    @Override
    public AttendeeQrCodeRespVO get(Long attendeeId) {
        EventAttendeeDO attendee = attendeeMapper.selectById(attendeeId);
        if (ObjectUtil.isNull(attendee)) {
            return null;
        }
        return AttendeeQrCodeRespVO.builder()
                .id(attendee.getId())
                .attendeeEmail(attendee.getAttendeeEmail())
                .attendeeMobile(attendee.getAttendeeMobile())
                .attendeeName(attendee.getAttendeeName())
                .checkInToken(attendee.getCheckInToken())
                .checkInStatus(attendee.getCheckInStatus())
                .build();
    }

    @Override
    public void delete(Long attendeeId) {
        EventAttendeeDO attendee = attendeeMapper.selectById(attendeeId);
        if (ObjectUtil.isEmpty(attendee)) {
            throw exception(ATTENDEE_NOT_EXIST);
        }
        attendeeMapper.deleteById(attendeeId);
    }

    @Override
    public AttendeeQrCodeRespVO update(Long attendeeId, AttendeeReqVO reqVO) {
        EventAttendeeDO attendee = attendeeMapper.selectById(attendeeId);
        if (ObjectUtil.isEmpty(attendee)) {
            throw exception(ATTENDEE_NOT_EXIST);
        }
        if (ObjectUtil.equals(attendee.getCheckInStatus(), 1)) {
            throw exception(UPDATE_ATTENDEE_FAILED);
        }
        EventDO event = eventMapper.selectById(attendee.getEventId());
        if (ObjectUtil.isEmpty(event)) {
            throw exception(EVENT_NOT_FOUND);
        }
        attendee.setAttendeeEmail(reqVO.getEmail());
        attendee.setAttendeeMobile(reqVO.getMobile());
        attendee.setAttendeeName(reqVO.getName());
        boolean isSuccess = attendeeMapper.updateById(attendee) > 0;
        if (!isSuccess) {
            throw exception(UPDATE_ATTENDEE_FAILED);
        }

        String token = attendee.getCheckInToken();
        if (ObjectUtil.isNull(attendee.getCheckInToken())) {
            token = UUID.randomUUID().toString();
            attendee.setCheckInToken(token);
            attendee.setQrCodeGeneratedTime(LocalDateTime.now());
            attendeeMapper.updateById(attendee);
            log.info(
                    "Generated new token for updating attendee: email={}",
                    attendee.getAttendeeEmail());
        }

        // Generate QR code
        QrCodeRespVO qrCode = qrCodeService.generateEventCheckInQrWithToken(token);
        String qrCodeUrl = baseUrl + "/system/attendee/scan?token=" + token;

        // Send email
        sendEmail(attendee, event, qrCode);

        return AttendeeQrCodeRespVO.builder()
                .id(attendee.getId())
                .attendeeEmail(attendee.getAttendeeEmail())
                .attendeeName(attendee.getAttendeeName())
                .attendeeMobile(attendee.getAttendeeMobile())
                .checkInToken(token)
                .qrCodeBase64(qrCode.getBase64Image())
                .qrCodeUrl(qrCodeUrl)
                .checkInStatus(attendee.getCheckInStatus())
                .build();
    }

    @Override
    @Transactional
    public CheckInRespVO checkIn(String token) {
        log.info("Starting check-in process with validation chain");

        // Build validation context
        CheckInValidationContext context = CheckInValidationContext.builder()
                .token(token)
                .currentTime(LocalDateTime.now())
                .validationFailed(false)
                .build();

        // Build and execute validation chain
        CheckInValidator validationChain = validationChainBuilder.buildValidationChain();
        validationChain.validate(context);

        // Check if validation failed
        if (context.isValidationFailed()) {
            log.warn("Check-in validation failed: {}", context.getErrorMessage());
            throw exception(VALIDATION_FAILED);
        }

        // All validations passed - perform check-in
        EventAttendeeDO attendee = context.getAttendee();
        EventDO event = context.getEvent();
        LocalDateTime now = context.getCurrentTime();

        attendee.setCheckInStatus(1);
        attendee.setCheckInTime(now);
        attendeeMapper.updateById(attendee);

        log.info(
                "Attendee {} ({}) checked in successfully for event {}",
                attendee.getAttendeeName(),
                attendee.getAttendeeEmail(),
                event.getName());

        return CheckInRespVO.builder()
                .eventId(event.getId())
                .eventName(event.getName())
                .userId(null)
                .userName(attendee.getAttendeeName())
                .checkInTime(now)
                .success(true)
                .message("Check-in successful!")
                .build();
    }

    /**
     * Map error message to error code constant
     */
    private int getErrorCodeFromMessage(String errorMessage) {
        return switch (errorMessage) {
            case "INVALID_CHECKIN_TOKEN" -> INVALID_CHECKIN_TOKEN.getCode();
            case "ALREADY_CHECKED_IN" -> ALREADY_CHECKED_IN.getCode();
            case "EVENT_NOT_FOUND" -> EVENT_NOT_FOUND.getCode();
            case "EVENT_NOT_ACTIVE" -> EVENT_NOT_ACTIVE.getCode();
            case "CHECKIN_NOT_STARTED" -> CHECKIN_NOT_STARTED.getCode();
            case "CHECKIN_ENDED" -> CHECKIN_ENDED.getCode();
            default -> INVALID_CHECKIN_TOKEN.getCode();
        };
    }

    @Override
    @Transactional
    public GenerateQrCodesRespVO generateQrCodesForAttendees(GenerateQrCodesReqVO reqVO) {
        Long eventId = reqVO.getEventId();
        List<AttendeeReqVO> attendeeInfos = reqVO.getAttendees();

        // 1. Validate event
        EventDO event = eventMapper.selectById(eventId);
        if (ObjectUtil.isNull(event)) {
            throw exception(EVENT_NOT_FOUND);
        }

        // 2. Process each attendee
        List<AttendeeQrCodeRespVO> successList = new ArrayList<>();
        List<String> failedList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (AttendeeReqVO info : attendeeInfos) {
            try {
                if (info.getEmail() == null || info.getEmail().isBlank()) {
                    failedList.add("email can't be empty");
                    continue;
                }

                EventAttendeeDO existing =
                        attendeeMapper.selectByEventAndEmail(eventId, info.getEmail());

                if (ObjectUtil.isNotNull(existing)) {
                    failedList.add(info.getEmail() + " - already exist in this event");
                    log.warn(
                            "Attendee already exists: email={}, eventId={}",
                            info.getEmail(),
                            eventId);
                    continue;
                }

                String token = UUID.randomUUID().toString();
                EventAttendeeDO attendee =
                        EventAttendeeDO.builder()
                                .eventId(eventId)
                                .attendeeEmail(info.getEmail())
                                .attendeeName(info.getName())
                                .attendeeMobile(info.getMobile())
                                .checkInToken(token)
                                .checkInStatus(0)
                                .qrCodeGeneratedTime(now)
                                .build();

                attendeeMapper.insert(attendee);

                QrCodeRespVO qrCode = qrCodeService.generateEventCheckInQrWithToken(token);
                String qrCodeUrl = baseUrl + "/system/attendee/scan?token=" + token;

                try {
                    sendEmail(attendee, event, qrCode);
                } catch (Exception e) {
                    log.error("Failed to send email to {}: {}", info.getEmail(), e.getMessage());
                }

                successList.add(
                        AttendeeQrCodeRespVO.builder()
                                .id(attendee.getId())
                                .attendeeEmail(attendee.getAttendeeEmail())
                                .attendeeName(attendee.getAttendeeName())
                                .attendeeMobile(attendee.getAttendeeMobile())
                                .checkInToken(token)
                                .qrCodeBase64(qrCode.getBase64Image())
                                .qrCodeUrl(qrCodeUrl)
                                .checkInStatus(0)
                                .build());

            } catch (Exception e) {
                log.error("Error processing attendee {}: {}", info.getEmail(), e.getMessage(), e);
                failedList.add(info.getEmail() + " - System error");
            }
        }

        log.info(
                "Event {}: {} succeeded, {} failed",
                eventId,
                successList.size(),
                failedList.size());

        if (!failedList.isEmpty()) {
            log.warn("Failed attendees: {}", failedList);
        }

        if (successList.isEmpty()) {
            String errorMsg = String.join("; ", failedList);
            throw exception(ATTENDEE_CREATION_FAILED, "Fail to create" + errorMsg);
        }

        return GenerateQrCodesRespVO.builder()
                .eventId(eventId)
                .eventName(event.getName())
                .totalCount(successList.size())
                .attendees(successList)
                .build();
    }

    @Override
    @Transactional
    public String getCheckInToken(Long eventId, String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }

        EventDO event = eventMapper.selectById(eventId);
        if (ObjectUtil.isNull(event)) {
            throw exception(EVENT_NOT_FOUND);
        }

        EventAttendeeDO attendee = attendeeMapper.selectByEventAndEmail(eventId, email);

        if (ObjectUtil.isNull(attendee)) {
            log.info("Attendee not found for eventId={}, email={}", eventId, email);
            throw exception(EVENT_ATTENDEE_NOT_FOUND);
        }

        if (ObjectUtil.isNull(attendee.getCheckInToken())) {
            String token = UUID.randomUUID().toString();
            attendee.setCheckInToken(token);
            attendee.setQrCodeGeneratedTime(LocalDateTime.now());
            attendeeMapper.updateById(attendee);
            log.info("Generated token for attendee: email={}", email);
        }

        return attendee.getCheckInToken();
    }

    private void sendEmail(EventAttendeeDO attendee, EventDO event, QrCodeRespVO qrCode) {
        byte[] qrCodeBytes = Base64.getDecoder().decode(qrCode.getBase64Image());
        TenantDO tenant = tenantMapper.selectById(getCurrentTenantId());
        AttendeeInviteReqVO emailReq =
                AttendeeInviteReqVO.builder()
                        .toEmail(attendee.getAttendeeEmail())
                        .attendeeMobile(attendee.getAttendeeMobile())
                        .attendeeName(attendee.getAttendeeName())
                        .qrCodeBytes(qrCodeBytes)
                        .qrCodeContentType(qrCode.getContentType())
                        .eventName(event.getName())
                        .eventDescription(event.getDescription())
                        .eventId(event.getId())
                        .eventLocation(event.getLocation())
                        .eventDate(event.getStartTime().toString())
                        .organizationName(ObjectUtil.isNotNull(tenant) ? tenant.getName() : null)
                        .build();
        attendeeEmailService.sendAttendeeInvite(emailReq);
    }

    @Override
    public AttendeeInfoRespVO getAttendeeInfo(String token) {
        if (token == null || token.isBlank()) {
            throw exception(INVALID_CHECKIN_TOKEN);
        }

        EventAttendeeDO attendee = attendeeMapper.selectByToken(token);
        if (ObjectUtil.isNull(attendee)) {
            throw exception(INVALID_CHECKIN_TOKEN);
        }

        EventDO event = eventMapper.selectById(attendee.getEventId());
        if (ObjectUtil.isNull(event)) {
            throw exception(EVENT_NOT_FOUND);
        }

        AttendeeInfoRespVO respVO =
                AttendeeInfoRespVO.builder()
                        .eventName(event.getName())
                        .attendeeName(attendee.getAttendeeName())
                        .attendeeEmail(attendee.getAttendeeEmail())
                        .checkInStatus(attendee.getCheckInStatus())
                        .checkInTime(attendee.getCheckInTime())
                        .build();

        log.info(
                "Attendee info retrieved: name={}, email={}, checkInStatus={}",
                attendee.getAttendeeName(),
                attendee.getAttendeeEmail(),
                attendee.getCheckInStatus());

        return respVO;
    }
}