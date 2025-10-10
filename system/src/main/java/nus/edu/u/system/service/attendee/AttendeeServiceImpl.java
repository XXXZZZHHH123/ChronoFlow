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
import nus.edu.u.system.domain.vo.attendee.AttendeeInviteReqVO;
import nus.edu.u.system.domain.vo.attendee.AttendeeQrCodeRespVO;
import nus.edu.u.system.domain.vo.attendee.AttendeeReqVO;
import nus.edu.u.system.domain.vo.checkin.CheckInRespVO;
import nus.edu.u.system.domain.vo.checkin.GenerateQrCodesReqVO;
import nus.edu.u.system.domain.vo.checkin.GenerateQrCodesRespVO;
import nus.edu.u.system.domain.vo.qrcode.QrCodeRespVO;
import nus.edu.u.system.enums.event.EventStatusEnum;
import nus.edu.u.system.mapper.attendee.EventAttendeeMapper;
import nus.edu.u.system.mapper.task.EventMapper;
import nus.edu.u.system.mapper.tenant.TenantMapper;
import nus.edu.u.system.service.email.AttendeeEmailService;
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
        // 1. Validate token
        EventAttendeeDO attendee = attendeeMapper.selectByToken(token);
        if (ObjectUtil.isNull(attendee)) {
            throw exception(INVALID_CHECKIN_TOKEN);
        }

        // 2. Check if already checked in
        if (ObjectUtil.equal(attendee.getCheckInStatus(), 1)) {
            throw exception(ALREADY_CHECKED_IN);
        }

        // 3. Validate event status and time
        EventDO event = eventMapper.selectById(attendee.getEventId());
        if (ObjectUtil.isNull(event)) {
            throw exception(EVENT_NOT_FOUND);
        }

        if (!ObjectUtil.equal(event.getStatus(), EventStatusEnum.DOING.getStatus())) {
            throw exception(EVENT_NOT_ACTIVE);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkInStart = event.getStartTime().minusHours(2);
        LocalDateTime checkInEnd = event.getEndTime();

        if (now.isBefore(checkInStart)) {
            throw exception(CHECKIN_NOT_STARTED);
        }
        if (now.isAfter(checkInEnd)) {
            throw exception(CHECKIN_ENDED);
        }

        // 4. Update check-in status
        attendee.setCheckInStatus(1);
        attendee.setCheckInTime(now);
        attendeeMapper.updateById(attendee);

        log.info(
                "Attendee {} ({}) checked in for event {}",
                attendee.getAttendeeName(),
                attendee.getAttendeeEmail(),
                event.getName());

        return CheckInRespVO.builder()
                .eventId(event.getId())
                .eventName(event.getName())
                .userId(null) // Attendee 没有 userId
                .userName(attendee.getAttendeeName())
                .checkInTime(now)
                .success(true)
                .message("Check-in successful!")
                .build();
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

        // 2. Generate tokens and QR codes for each attendee
        List<AttendeeQrCodeRespVO> attendeeQrCodes = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (AttendeeReqVO info : attendeeInfos) {
            if (info.getEmail() == null || info.getEmail().isBlank()) {
                log.warn("Skipping attendee with empty email");
                continue;
            }

            // Check if attendee already exists
            EventAttendeeDO attendee =
                    attendeeMapper.selectByEventAndEmail(eventId, info.getEmail());

            String token;
            if (ObjectUtil.isNull(attendee)) {
                // Create new attendee with unique token
                token = UUID.randomUUID().toString();

                attendee =
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
                log.info("Created attendee record: email={}, eventId={}", info.getEmail(), eventId);
            } else {
                // Use existing token or generate new one if null
                if (ObjectUtil.isNull(attendee.getCheckInToken())) {
                    token = UUID.randomUUID().toString();
                    attendee.setCheckInToken(token);
                    attendee.setQrCodeGeneratedTime(now);
                    attendeeMapper.updateById(attendee);
                    log.info(
                            "Generated new token for existing attendee: email={}", info.getEmail());
                } else {
                    token = attendee.getCheckInToken();
                    log.info("Using existing token for attendee: email={}", info.getEmail());
                }
            }

            // Generate QR code
            QrCodeRespVO qrCode = qrCodeService.generateEventCheckInQrWithToken(token);
            String qrCodeUrl = baseUrl + "/system/attendee/scan?token=" + token;

            // Send email
            sendEmail(attendee, event, qrCode);

            AttendeeQrCodeRespVO attendeeQr =
                    AttendeeQrCodeRespVO.builder()
                            .id(attendee.getId())
                            .attendeeEmail(attendee.getAttendeeEmail())
                            .attendeeName(attendee.getAttendeeName())
                            .attendeeMobile(attendee.getAttendeeMobile())
                            .checkInToken(token)
                            .qrCodeBase64(qrCode.getBase64Image())
                            .qrCodeUrl(qrCodeUrl)
                            .checkInStatus(attendee.getCheckInStatus())
                            .build();

            attendeeQrCodes.add(attendeeQr);
        }

        log.info("Generated {} QR codes for event {}", attendeeQrCodes.size(), eventId);

        return GenerateQrCodesRespVO.builder()
                .eventId(eventId)
                .eventName(event.getName())
                .totalCount(attendeeQrCodes.size())
                .attendees(attendeeQrCodes)
                .build();
    }

    @Override
    @Transactional
    public String getCheckInToken(Long eventId, String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }

        // Validate event exists
        EventDO event = eventMapper.selectById(eventId);
        if (ObjectUtil.isNull(event)) {
            throw exception(EVENT_NOT_FOUND);
        }

        // Find attendee by email
        EventAttendeeDO attendee = attendeeMapper.selectByEventAndEmail(eventId, email);

        if (ObjectUtil.isNull(attendee)) {
            // Attendee doesn't exist yet - will be created when sending email
            log.info("Attendee not found for eventId={}, email={}", eventId, email);
            throw exception(EVENT_ATTENDEE_NOT_FOUND);
        }

        // Generate token if doesn't exist
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
}
