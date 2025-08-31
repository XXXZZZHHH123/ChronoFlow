package nus.edu.u.framework.security.filter;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import nus.edu.u.common.core.domain.CommonResult;
import nus.edu.u.common.core.domain.LoginUser;
import nus.edu.u.common.exception.enums.GlobalErrorCodeConstants;
import nus.edu.u.common.utils.security.JwtUtils;
import nus.edu.u.common.utils.security.SecurityUtils;
import nus.edu.u.common.utils.servlet.ServletUtils;
import nus.edu.u.framework.security.config.SecurityProperties;
import nus.edu.u.framework.web.handler.GlobalExceptionHandler;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static nus.edu.u.common.constant.CacheConstants.LOGIN_ACCESS_TOKEN_KEY;

/**
 * Token filter, valid token
 * After authentication, get {@link LoginUser} info and add into Spring Security context
 *
 * @author Lu Shuwen
 * @date 2025-08-29
 */
@Component
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    private final SecurityProperties securityProperties;

    private final GlobalExceptionHandler globalExceptionHandler;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @SuppressWarnings("NullableProblems")
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Get token from request
        String token = SecurityUtils.getAuthorization(request, securityProperties.getTokenHeader(),
                securityProperties.getTokenParameter());
        if (StrUtil.isNotEmpty(token)) {
            try {
                // Build LoginUser from token
                LoginUser loginUser = buildLoginUserByToken(token);
                // Build mock LoginUser
                if (loginUser == null) {
                    loginUser = mockLoginUser(request, token);
                }
                // Set current user
                if (loginUser != null) {
                    // Check if token exists in redis
                    if (stringRedisTemplate.hasKey(LOGIN_ACCESS_TOKEN_KEY + loginUser.getId())) {
                        SecurityUtils.setLoginUser(loginUser, request);
                    }
                }
            } catch (Exception ex) {
                CommonResult<?> result = globalExceptionHandler.allExceptionHandler(request, ex);
                ServletUtils.writeJSON(response, result);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private LoginUser buildLoginUserByToken(String token) {
        if (!jwtUtils.validateToken(token)) {
            return null;
        }
        return LoginUser.builder().id(jwtUtils.getUserId(token))
                .roleId(jwtUtils.getRoleId(token))
                .tenantId(jwtUtils.getTenantId(token))
                .expireTime(jwtUtils.getExpireTime(token))
                .build();
    }

    private LoginUser mockLoginUser(HttpServletRequest request, String token) {
        if (!securityProperties.getMockEnable()) {
            return null;
        }
        // token start with mock secret key
        if (!token.startsWith(securityProperties.getMockSecret())) {
            return null;
        }
        try {
            // Build mock user  [mock secret key]-[userId]-[tenantId]-[roleId]-[expireTime]
            Long[] userInfo = Arrays.stream(token.substring(securityProperties.getMockSecret().length())
                    .split("-")).map(NumberUtil::parseLong).toArray(Long[]::new);
            return LoginUser.builder()
                    .id(userInfo[0])
                    .tenantId(userInfo[1])
                    .roleId(userInfo[2])
                    .expireTime(userInfo[3])
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

}
