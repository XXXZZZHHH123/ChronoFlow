package nus.edu.u.common.utils.security;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import nus.edu.u.common.core.domain.LoginUser;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import java.util.Collections;

/**
 * Security Service utils class
 *
 * @author Lu Shuwen
 * @date 2025-08-29
 */
public class SecurityUtils {

    /**
     * Header
     */
    public static final String AUTHORIZATION_HEADER = "Bearer";

    private SecurityUtils() {}

    /**
     * Get token from HTTP request
     *
     * @param request HTTP request
     * @param headerName The header name of token
     * @param parameterName The Parameter name of token
     * @return token
     */
    @Nullable
    public static String getAuthorization(HttpServletRequest request, String headerName, String parameterName) {
        // Get token from header then from parameter
        String token = request.getHeader(headerName);
        if (StrUtil.isEmpty(token)) {
            token = request.getParameter(parameterName);
        }
        if (StrUtil.isEmpty(token)) {
            return null;
        }
        int index = token.indexOf(AUTHORIZATION_HEADER + " ");
        return index >= 0 ? token.substring(index + AUTHORIZATION_HEADER.length() + 1) : token;
    }

    /**
     * Set current user into context
     *
     * @param loginUser Login user object
     * @param request HTTP request
     */
    public static void setLoginUser(LoginUser loginUser, HttpServletRequest request) {
        // Create Authentication and then set into context
        Authentication authentication = buildAuthentication(loginUser, request);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static Authentication buildAuthentication(LoginUser loginUser, HttpServletRequest request) {
        // Create UsernamePasswordAuthenticationToken object
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginUser, null, Collections.emptyList());
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return authenticationToken;
    }

    /**
     * Get current authentication info
     *
     * @return Authentication info
     */
    public static Authentication getAuthentication() {
        SecurityContext context = SecurityContextHolder.getContext();
        if (context == null) {
            return null;
        }
        return context.getAuthentication();
    }

    /**
     * Get current user
     *
     * @return login user
     */
    @Nullable
    public static LoginUser getLoginUser() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return null;
        }
        return authentication.getPrincipal() instanceof LoginUser ? (LoginUser) authentication.getPrincipal() : null;
    }

    /**
     * Get current user id
     *
     * @return userId
     */
    @Nullable
    public static Long getLoginUserId() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getId() : null;
    }

    /**
     * Get current user tenant id
     *
     * @return tenantId
     */
    @Nullable
    public static Long getLonginUserTenantId() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getTenantId() : null;
    }

    /**
     * Get current user role id
     *
     * @return roleId
     */
    @Nullable
    public static Long getLonginUserRoleId() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getRoleId() : null;
    }

}
