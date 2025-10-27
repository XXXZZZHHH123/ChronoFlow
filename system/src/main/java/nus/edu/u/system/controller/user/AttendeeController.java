package nus.edu.u.system.controller.user;

import static nus.edu.u.common.constant.PermissionConstants.CREATE_MEMBER;
import static nus.edu.u.common.core.domain.CommonResult.success;
import static nus.edu.u.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.core.domain.CommonResult;
import nus.edu.u.system.domain.vo.attendee.AttendeeInfoRespVO;
import nus.edu.u.system.domain.vo.attendee.AttendeeQrCodeRespVO;
import nus.edu.u.system.domain.vo.attendee.AttendeeReqVO;
import nus.edu.u.system.domain.vo.checkin.CheckInReqVO;
import nus.edu.u.system.domain.vo.checkin.CheckInRespVO;
import nus.edu.u.system.domain.vo.checkin.GenerateQrCodesReqVO;
import nus.edu.u.system.domain.vo.checkin.GenerateQrCodesRespVO;
import nus.edu.u.system.service.attendee.AttendeeService;
import nus.edu.u.system.service.excel.ExcelService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/system/attendee")
@Validated
@Slf4j
public class AttendeeController {

    @Resource private AttendeeService attendeeService;

    @Resource private ExcelService excelService;

    @GetMapping("list/{eventId}")
    public CommonResult<List<AttendeeQrCodeRespVO>> list(
            @PathVariable("eventId") @NotNull Long eventId) {
        return success(attendeeService.list(eventId));
    }

    @GetMapping("/{attendeeId}")
    public CommonResult<AttendeeQrCodeRespVO> get(
            @PathVariable("attendeeId") @NotNull Long attendeeId) {
        return success(attendeeService.get(attendeeId));
    }

    @DeleteMapping("/{attendeeId}")
    public CommonResult<Boolean> delete(@PathVariable("attendeeId") @NotNull Long attendeeId) {
        attendeeService.delete(attendeeId);
        return success(true);
    }

    @PatchMapping("/{attendeeId}")
    public CommonResult<AttendeeQrCodeRespVO> update(
            @PathVariable("attendeeId") @NotNull Long attendeeId,
            @RequestBody @Valid AttendeeReqVO reqVO) {
        return success(attendeeService.update(attendeeId, reqVO));
    }

    /** Generate batch QR codes */
    @SaCheckPermission(CREATE_MEMBER)
    @PostMapping
    public CommonResult<GenerateQrCodesRespVO> generateQrCodes(
            @RequestBody @Valid GenerateQrCodesReqVO reqVO) {
        log.info(
                "Generating QR codes for event {} with {} attendees",
                reqVO.getEventId(),
                reqVO.getAttendees().size());
        GenerateQrCodesRespVO response = attendeeService.generateQrCodesForAttendees(reqVO);
        return success(response);
    }

    @SaCheckPermission(CREATE_MEMBER)
    @PostMapping("/bulk/{eventId}")
    public CommonResult<GenerateQrCodesRespVO> generateQrCodeByExcel(
            @PathVariable("eventId") @NotNull Long eventId,
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw exception(BAD_REQUEST);
        }
        List<AttendeeReqVO> attendees = excelService.importAttendees(file);
        GenerateQrCodesReqVO reqVO = new GenerateQrCodesReqVO();
        reqVO.setAttendees(attendees);
        reqVO.setEventId(eventId);
        GenerateQrCodesRespVO response = attendeeService.generateQrCodesForAttendees(reqVO);
        return success(response);
    }

    @SaIgnore
    @GetMapping("/scan")
    public CommonResult<AttendeeInfoRespVO> attendeePreview(@RequestParam("token") String token) {
        log.info(
                "Attendee preview request with token: {}",
                token.substring(0, Math.min(8, token.length())) + "...");

        AttendeeInfoRespVO info = attendeeService.getAttendeeInfo(token);
        info.setMessage("please show this QR code to the on-site staff for scanning");
        return success(info);
    }

    @PostMapping("/staff-scan")
    public CommonResult<CheckInRespVO> checkIn(@RequestBody @Valid CheckInReqVO reqVO) {
        log.info(
                "Check-in request with token: {}",
                reqVO.getToken().substring(0, Math.min(8, reqVO.getToken().length())) + "...");
        CheckInRespVO response = attendeeService.checkIn(reqVO.getToken());
        return success(response);
    }
}
