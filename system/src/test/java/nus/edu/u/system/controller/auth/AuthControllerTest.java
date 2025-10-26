package nus.edu.u.system.controller.auth;

import static nus.edu.u.common.constant.SecurityConstants.REFRESH_TOKEN_COOKIE_NAME;
import static nus.edu.u.common.constant.SecurityConstants.REFRESH_TOKEN_REMEMBER_COOKIE_MAX_AGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.Cookie;
import nus.edu.u.common.core.domain.CommonResult;
import nus.edu.u.framework.security.config.CookieConfig;
import nus.edu.u.system.domain.vo.auth.LoginReqVO;
import nus.edu.u.system.domain.vo.auth.LoginRespVO;
import nus.edu.u.system.service.auth.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthService authService;
    @Mock private CookieConfig cookieConfig;

    @InjectMocks private AuthController authController;

    @Test
    void login_withRememberFlag_setsLongLifeCookie() {
        when(cookieConfig.isHttpOnly()).thenReturn(true);
        when(cookieConfig.isSecurity()).thenReturn(false);

        LoginReqVO reqVO =
                LoginReqVO.builder()
                        .username("member01")
                        .password("password123")
                        .remember(true)
                        .build();
        LoginRespVO loginRespVO = LoginRespVO.builder().refreshToken("new-token").build();
        when(authService.login(any(LoginReqVO.class))).thenReturn(loginRespVO);

        MockHttpServletResponse response = new MockHttpServletResponse();

        CommonResult<LoginRespVO> result = authController.login(reqVO, "old-token", response);

        assertThat(result.getData().getRefreshToken()).isEqualTo("new-token");

        ArgumentCaptor<LoginReqVO> captor = ArgumentCaptor.forClass(LoginReqVO.class);
        verify(authService).login(captor.capture());
        assertThat(captor.getValue().getRefreshToken()).isEqualTo("old-token");

        Cookie cookie = response.getCookie(REFRESH_TOKEN_COOKIE_NAME);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getMaxAge()).isEqualTo(REFRESH_TOKEN_REMEMBER_COOKIE_MAX_AGE);
        assertThat(cookie.getValue()).isEqualTo("new-token");
    }

    @Test
    void logout_clearsRefreshCookie() {
        when(cookieConfig.isHttpOnly()).thenReturn(true);
        when(cookieConfig.isSecurity()).thenReturn(true);

        MockHttpServletResponse response = new MockHttpServletResponse();

        CommonResult<Boolean> result = authController.logout("refresh-value", response);

        assertThat(result.getData()).isTrue();
        verify(authService).logout("refresh-value");
        Cookie cookie = response.getCookie(REFRESH_TOKEN_COOKIE_NAME);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getMaxAge()).isZero();
    }

    @Test
    void refresh_returnsNewTokens() {
        LoginRespVO respVO = LoginRespVO.builder().refreshToken("newer-token").build();
        when(authService.refresh("old-token")).thenReturn(respVO);

        CommonResult<LoginRespVO> result = authController.refresh("old-token");

        assertThat(result.getData().getRefreshToken()).isEqualTo("newer-token");
    }
}
