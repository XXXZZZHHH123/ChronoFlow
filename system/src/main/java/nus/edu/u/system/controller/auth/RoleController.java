package nus.edu.u.system.controller.auth;

import static nus.edu.u.common.constant.PermissionConstants.*;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.core.domain.CommonResult;
import nus.edu.u.system.domain.vo.role.RoleAssignReqVO;
import nus.edu.u.system.domain.vo.role.RoleReqVO;
import nus.edu.u.system.domain.vo.role.RoleRespVO;
import nus.edu.u.system.service.role.RoleService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Role Controller
 *
 * @author Lu Shuwen
 * @date 2025-09-13
 */
@RestController
@RequestMapping("/system/roles")
@Validated
@Slf4j
public class RoleController {

    @Resource private RoleService roleService;

    @GetMapping
    public CommonResult<List<RoleRespVO>> listRoles() {
        return CommonResult.success(roleService.listRoles());
    }

    @SaCheckPermission(CREATE_ROLE)
    @PostMapping
    public CommonResult<RoleRespVO> createRole(@RequestBody @Valid RoleReqVO roleReqVO) {
        return CommonResult.success(roleService.createRole(roleReqVO));
    }

    @GetMapping("/{roleId}")
    public CommonResult<RoleRespVO> getRole(@PathVariable("roleId") Long roleId) {
        return CommonResult.success(roleService.getRole(roleId));
    }

    @SaCheckPermission(DELETE_ROLE)
    @DeleteMapping("/{roleId}")
    public CommonResult<Boolean> deleteRole(@PathVariable("roleId") Long roleId) {
        roleService.deleteRole(roleId);
        return CommonResult.success(true);
    }

    @SaCheckPermission(UPDATE_ROLE)
    @PatchMapping("/{roleId}")
    public CommonResult<RoleRespVO> updateRole(
            @PathVariable("roleId") Long roleId, @RequestBody @Valid RoleReqVO roleReqVO) {
        return CommonResult.success(roleService.updateRole(roleId, roleReqVO));
    }

    @SaCheckPermission(ASSIGN_ROLE)
    @PostMapping("/assign")
    public CommonResult<Boolean> assignRole(@RequestBody RoleAssignReqVO reqVO) {
        roleService.assignRoles(reqVO);
        return CommonResult.success(true);
    }
}
