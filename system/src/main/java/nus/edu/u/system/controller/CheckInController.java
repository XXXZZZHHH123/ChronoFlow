package nus.edu.u.system.controller;

import static nus.edu.u.common.constant.PermissionConstants.CREATE_MEMBER;
import static nus.edu.u.common.core.domain.CommonResult.success;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.core.domain.CommonResult;
import nus.edu.u.system.domain.vo.checkin.CheckInReqVO;
import nus.edu.u.system.domain.vo.checkin.CheckInRespVO;
import nus.edu.u.system.domain.vo.checkin.GenerateQrCodesReqVO;
import nus.edu.u.system.domain.vo.checkin.GenerateQrCodesRespVO;
import nus.edu.u.system.service.checkin.CheckInService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/checkin")
@Validated
@Slf4j
public class CheckInController {
    @Resource private CheckInService checkInService;

    /** Generate batch QR codes */
    @SaCheckPermission(CREATE_MEMBER)
    @PostMapping("/generate-qrcodes")
    public CommonResult<GenerateQrCodesRespVO> generateQrCodes(
            @RequestBody @Valid GenerateQrCodesReqVO reqVO) {
        log.info(
                "Generating QR codes for event {} with {} attendees",
                reqVO.getEventId(),
                reqVO.getAttendees().size());
        GenerateQrCodesRespVO response = checkInService.generateQrCodesForAttendees(reqVO);
        return success(response);
    }

    @SaIgnore
    @PostMapping("/scan")
    public CommonResult<CheckInRespVO> checkIn(@RequestBody @Valid CheckInReqVO reqVO) {
        log.info(
                "Check-in request with token: {}",
                reqVO.getToken().substring(0, Math.min(8, reqVO.getToken().length())) + "...");
        CheckInRespVO response = checkInService.checkIn(reqVO.getToken());
        return success(response);
    }

    @SaIgnore
    @GetMapping("/scan")
    public CommonResult<CheckInRespVO> checkInByGet(@RequestParam String token) {
        log.info(
                "Check-in request (GET) with token: {}",
                token.substring(0, Math.min(8, token.length())) + "...");
        CheckInRespVO response = checkInService.checkIn(token);
        return success(response);
    }
}
