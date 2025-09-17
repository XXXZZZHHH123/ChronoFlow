package nus.edu.u.system.controller.auth;

import static nus.edu.u.common.constant.SecurityConstants.REFRESH_TOKEN_COOKIE_NAME;
import static nus.edu.u.common.constant.SecurityConstants.REFRESH_TOKEN_REMEMBER_COOKIE_MAX_AGE;
import static nus.edu.u.common.core.domain.CommonResult.error;
import static nus.edu.u.common.core.domain.CommonResult.success;
import static nus.edu.u.common.exception.enums.GlobalErrorCodeConstants.MISSING_COOKIE;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.core.domain.CommonResult;
import nus.edu.u.framework.security.config.CookieConfig;
import nus.edu.u.framework.security.factory.AbstractCookieFactory;
import nus.edu.u.framework.security.factory.LongLifeRefreshTokenCookie;
import nus.edu.u.framework.security.factory.ZeroLifeRefreshTokenCookie;
import nus.edu.u.system.domain.vo.auth.LoginReqVO;
import nus.edu.u.system.domain.vo.auth.LoginRespVO;
import nus.edu.u.system.service.auth.AuthService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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

    @Resource private AuthService authService;

    @Resource private CookieConfig cookieConfig;

    @PostMapping("/login")
    public CommonResult<LoginRespVO> login(
            @RequestBody @Valid LoginReqVO reqVO,
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        reqVO.setRefreshToken(refreshToken);
        LoginRespVO loginRespVO = authService.login(reqVO);
        AbstractCookieFactory cookieFactory;
        if (reqVO.isRemember()) {
            cookieFactory =
                    new LongLifeRefreshTokenCookie(
                            cookieConfig.isHttpOnly(),
                            cookieConfig.isSecurity(),
                            REFRESH_TOKEN_REMEMBER_COOKIE_MAX_AGE);
        } else {
            cookieFactory =
                    new ZeroLifeRefreshTokenCookie(
                            cookieConfig.isHttpOnly(), cookieConfig.isSecurity());
        }
        response.addCookie(cookieFactory.createCookie(loginRespVO.getRefreshToken()));
        return success(loginRespVO);
    }

    @PostMapping("/logout")
    public CommonResult<Boolean> logout(
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(refreshToken);
        // Delete refresh token from cookie
        AbstractCookieFactory cookieFactory =
                new ZeroLifeRefreshTokenCookie(
                        cookieConfig.isHttpOnly(), cookieConfig.isSecurity());
        response.addCookie(cookieFactory.createCookie(null));
        return success(true);
    }

    @PostMapping("/refresh")
    public CommonResult<LoginRespVO> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken) {
        if (StrUtil.isBlank(refreshToken)) {
            return error(MISSING_COOKIE);
        }
        return success(authService.refresh(refreshToken));
    }
}
