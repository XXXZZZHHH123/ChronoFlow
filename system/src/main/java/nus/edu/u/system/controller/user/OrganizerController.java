package nus.edu.u.system.controller.user;


import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.core.domain.CommonResult;
import nus.edu.u.system.convert.user.UserConvert;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.dataobject.user.UserRoleDO;
import nus.edu.u.system.domain.dto.CreateUserDTO;
import nus.edu.u.system.domain.dto.UpdateUserDTO;
import nus.edu.u.system.domain.vo.user.*;
import nus.edu.u.system.mapper.user.UserRoleMapper;
import nus.edu.u.system.service.user.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @Resource
    private UserRoleMapper userRoleMapper;

    @PostMapping("/create/user")
    public CommonResult<Long> createUserForOrganizer(
            @Valid @RequestBody CreateUserReqVO req) {
        CreateUserDTO dto = userConvert.toDTO(req);
        Long userId = userService.createUserWithRoleIds(dto);
        return CommonResult.success(userId);
    }

    @PatchMapping("/update/user/{id}")
    public CommonResult<UpdateUserRespVO> updateUserForOrganizer(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateUserReqVO vo) {
        UpdateUserDTO dto = userConvert.toDTO(vo);
        dto.setId(id);

        UserDO updated = userService.updateUserWithRoleIds(dto);
        // Query the user's role ID
        List<Long> roleIds = userRoleMapper.selectList(
                Wrappers.<UserRoleDO>lambdaQuery()
                        .eq(UserRoleDO::getUserId, updated.getId())
                        .eq(UserRoleDO::getDeleted, false) // Only take the undeleted ones
        ).stream().map(UserRoleDO::getRoleId).toList();

        UpdateUserRespVO respVO = userConvert.toUpdateUserRespVO(updated);
        respVO.setRoleIds(roleIds);
        return CommonResult.success(respVO);
    }

    @DeleteMapping("/delete/user/{id}")
    public CommonResult<Boolean> softDeleteUser(@PathVariable("id") Long id) {
        userService.softDeleteUser(id);
        return CommonResult.success(Boolean.TRUE);
    }

    @PatchMapping("/restore/user/{id}")
    public CommonResult<Boolean> restoreUser(@PathVariable("id") Long id) {
        userService.restoreUser(id);
        return CommonResult.success(Boolean.TRUE);
    }

    @PatchMapping("/disable/user/{id}")
    public CommonResult<Boolean> disableUser(@PathVariable("id") Long id) {
        userService.disableUser(id);
        return CommonResult.success(true);
    }

    @PatchMapping("/enable/user/{id}")
    public CommonResult<Boolean> enableUser(@PathVariable("id") Long id) {
        userService.enableUser(id);
        return CommonResult.success(true);
    }

    @GetMapping("/users")
    public CommonResult<List<UserProfileRespVO>> getAllUserProfiles() {
        return CommonResult.success(userService.getAllUserProfiles());
    }



    //    @PostMapping("/create/profile")
//    public CommonResult<UserCreateRespVO> createUser(@Valid @RequestBody UserCreateReqVO reqVO) {
//        var dto = userConvert.toDTO(reqVO);
//        UserDO user = userService.createUser(dto);
//        UserCreateRespVO respVO = userConvert.toCreateRespVO(user);
//        return CommonResult.success(respVO);
//    }
//
//    @PatchMapping("/update/profile/{id}")
//    public CommonResult<UserUpdateRespVO> updateUser(
//            @PathVariable("id") Long id,
//            @Valid @RequestBody UserUpdateReqVO reqVO) {
//
//        // VO -> DTO（并补上 id）
//        UserUpdateDTO dto = userConvert.toUpdateDTO(reqVO);
//        dto.setId(id);
//
//        // 调用 Service，拿到最新 DO
//        UserDO updated = userService.updateUser(dto);
//
//        // DO -> RespVO
//        return CommonResult.success(userConvert.toUpdateRespVO(updated));
//    }
}
