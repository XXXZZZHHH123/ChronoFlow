package nus.edu.u.system.controller.auth;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.core.domain.CommonResult;
import nus.edu.u.common.utils.security.SecurityUtils;
import nus.edu.u.framework.security.config.SecurityProperties;
import nus.edu.u.system.domain.dataobject.dept.DeptDO;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.vo.auth.RefreshTokenVO;
import nus.edu.u.system.mapper.dept.DeptMapper;
import nus.edu.u.system.mapper.user.UserMapper;
import nus.edu.u.system.service.auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import nus.edu.u.system.domain.vo.auth.LoginReqVO;
import nus.edu.u.system.domain.vo.auth.LoginRespVO;

import java.util.List;

import static nus.edu.u.common.core.domain.CommonResult.error;
import static nus.edu.u.common.core.domain.CommonResult.success;
import static nus.edu.u.common.exception.enums.GlobalErrorCodeConstants.MISSING_COOKIE;

/**
 * Authentication controller
 *
 * @author Lu Shuwen
 * @date 2025-08-30
 */
@RestController
@RequestMapping("/system/auth")
@Validated
@Slf4j
public class AuthController {

    @Resource
    private AuthService authService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private DeptMapper deptMapper;

    @Resource
    private SecurityProperties securityProperties;

    @PostMapping("/login")
    public CommonResult<LoginRespVO> login(@RequestBody @Valid LoginReqVO reqVO, HttpServletResponse response) {
        LoginRespVO loginRespVO = authService.login(reqVO);
        // TODO Remove this to service layer
        Cookie refreshCookie = new Cookie("refreshToken", loginRespVO.getRefreshToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 Days
        response.addCookie(refreshCookie);
        return success(loginRespVO);
    }

    @PostMapping("/logout")
    public CommonResult<Boolean> logout(HttpServletRequest request, HttpServletResponse response) {
        // Logout
        String token = SecurityUtils.getAuthorization(request,
                securityProperties.getTokenHeader(), securityProperties.getTokenParameter());
        if (StrUtil.isNotBlank(token)) {
            authService.logout(token);
        }
        // Delete refresh token from cookie
        Cookie deleteCookie = new Cookie("refreshToken", null);
        deleteCookie.setHttpOnly(true);
        deleteCookie.setPath("/");
        deleteCookie.setMaxAge(0);
        response.addCookie(deleteCookie);
        return success(true);
    }

    @PostMapping("/refresh")
    public CommonResult<LoginRespVO> refresh(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
        if (StrUtil.isBlank(refreshToken)) {
            return error(MISSING_COOKIE);
        }
        return success(authService.refresh(refreshToken));
    }

    @GetMapping("/list")
    public List<UserDO> list() {
        return userMapper.selectList(null);
    }

}
