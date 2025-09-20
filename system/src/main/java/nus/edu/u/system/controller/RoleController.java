package nus.edu.u.system.controller;

import jakarta.annotation.Resource;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.core.domain.CommonResult;
import nus.edu.u.system.domain.vo.role.RoleListRespVO;
import nus.edu.u.system.service.role.RoleService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Role Controller
 *
 * @author Lu Shuwen
 * @date 2025-09-13
 */
@RestController
@RequestMapping("/system/role")
@Validated
@Slf4j
public class RoleController {
    @Resource private RoleService roleService;

    @GetMapping("/list")
    public CommonResult<List<RoleListRespVO>> listRoles() {
        return CommonResult.success(roleService.listRolesExcludingAdmin());
    }
}
