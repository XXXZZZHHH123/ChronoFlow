package nus.edu.u.system.controller.user;


import cn.dev33.satoken.annotation.SaCheckRole;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.core.domain.CommonResult;
import nus.edu.u.system.convert.user.UserConvert;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.dto.UserUpdateDTO;
import nus.edu.u.system.domain.vo.user.UserCreateReqVO;
import nus.edu.u.system.domain.vo.user.UserCreateRespVO;
import nus.edu.u.system.domain.vo.user.UserUpdateReqVO;
import nus.edu.u.system.domain.vo.user.UserUpdateRespVO;
import nus.edu.u.system.service.user.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/organizer")
@Validated
@Slf4j
@SaCheckRole("ORGANIZER")
public class OrganizerController {
    @Resource
    private UserService userService;

    @Resource
    private UserConvert userConvert;

    @PostMapping("/createUser")
    public CommonResult<UserCreateRespVO> createUser(@Valid @RequestBody UserCreateReqVO reqVO) {
//        log.info("reqVO = {}", reqVO);
        var dto = userConvert.toDTO(reqVO);
//        if (dto == null) {
//            log.error("UserConvert.toDTO 返回了 null, reqVO = {}", reqVO);
//        } else {
//            log.info("UserConvert.toDTO 成功, dto = {}", dto);
//        }
        UserDO user = userService.createUser(dto);
        UserCreateRespVO respVO = userConvert.toCreateRespVO(user);
        return CommonResult.success(respVO);
    }

    @PatchMapping("/updateUser/{id}")
    public CommonResult<UserUpdateRespVO> updateUser(
            @PathVariable("id") Long id,
            @Valid @RequestBody UserUpdateReqVO reqVO) {

        // VO -> DTO（并补上 id）
        UserUpdateDTO dto = userConvert.toUpdateDTO(reqVO);
        dto.setId(id);

        // 调用 Service，拿到最新 DO
        UserDO updated = userService.updateUser(dto);

        // DO -> RespVO
        return CommonResult.success(userConvert.toUpdateRespVO(updated));
    }
}
