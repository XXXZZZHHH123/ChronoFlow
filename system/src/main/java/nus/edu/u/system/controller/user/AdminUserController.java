package nus.edu.u.system.controller.user;


import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.core.domain.CommonResult;
import nus.edu.u.system.domain.dto.UserCreateDTO;
import nus.edu.u.system.mapper.user.UserMapper;
import nus.edu.u.system.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
@Validated
@Slf4j
@SaCheckRole("ADMIN")
public class AdminUserController {
    @Resource
    private UserService userService;

    @PostMapping
    public CommonResult<Long> createUser(@Valid @RequestBody UserCreateDTO dto) {
        Long userId = userService.createUser(dto);
        return CommonResult.success(userId);
    }
}
