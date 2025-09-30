package nus.edu.u.system.controller.auth;

import static nus.edu.u.common.core.domain.CommonResult.success;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.core.domain.CommonResult;
import nus.edu.u.system.domain.vo.permission.PermissionReqVO;
import nus.edu.u.system.domain.vo.permission.PermissionRespVO;
import nus.edu.u.system.service.permission.PermissionService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author Lu Shuwen
 * @date 2025-09-29
 */
@RestController
@RequestMapping("/system/permissions")
@Validated
@Slf4j
public class PermissionController {

    @Resource private PermissionService permissionService;

    @SaCheckRole(
            value = {"ORGANIZER", "ADMIN"},
            mode = SaMode.OR)
    @GetMapping
    public CommonResult<List<PermissionRespVO>> list() {
        return success(permissionService.listPermissions());
    }

    @SaCheckRole("ADMIN")
    @PostMapping
    public CommonResult<Long> create(@RequestBody @Valid PermissionReqVO reqVO) {
        return success(permissionService.createPermission(reqVO));
    }

    @SaCheckRole(
            value = {"ORGANIZER", "ADMIN"},
            mode = SaMode.OR)
    @GetMapping("/{id}")
    public CommonResult<PermissionRespVO> getPermission(@PathVariable("id") Long id) {
        return success(permissionService.getPermission(id));
    }

    @SaCheckRole("ADMIN")
    @PatchMapping("/{id}")
    public CommonResult<PermissionRespVO> update(
            @PathVariable("id") Long id, @RequestBody @Valid PermissionReqVO reqVO) {
        return success(permissionService.updatePermission(id, reqVO));
    }

    @SaCheckRole("ADMIN")
    @DeleteMapping("/{id}")
    public CommonResult<Boolean> delete(@PathVariable("id") Long id) {
        return success(permissionService.deletePermission(id));
    }
}
