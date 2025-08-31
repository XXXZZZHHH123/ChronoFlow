package nus.edu.u.common.utils.security;

import cn.hutool.core.util.StrUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import jakarta.annotation.Resource;
import nus.edu.u.common.config.JwtProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static nus.edu.u.common.constant.SecurityConstants.*;

/**
 * Jwt token utils
 *
 * @author Lu Shuwen
 * @date 2025-08-28
 */
@Component
public class JwtUtils {

    @Resource
    private JwtProperties jwtProperties;

    public String generateAccessToken(Long userId, Long roleId, Long tenantId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put(JWT_PAYLOAD_USER_ID, userId);
        payload.put(JWT_PAYLOAD_ROLE_ID, roleId);
        payload.put(JWT_PAYLOAD_TENANT_ID, tenantId);
        payload.put(JWT_PAYLOAD_EXPIRE_TIME, System.currentTimeMillis() + jwtProperties.getAccessExpire() * 1000);

        return JWTUtil.createToken(payload, jwtProperties.getSecret().getBytes());
    }

    public String generateRefreshToken(Long userId, Long roleId, Long tenantId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put(JWT_PAYLOAD_USER_ID, userId);
        payload.put(JWT_PAYLOAD_ROLE_ID, roleId);
        payload.put(JWT_PAYLOAD_TENANT_ID, tenantId);
        payload.put(JWT_PAYLOAD_EXPIRE_TIME, System.currentTimeMillis() + jwtProperties.getRefreshExpire() * 1000);

        return JWTUtil.createToken(payload, jwtProperties.getSecret().getBytes());
    }

    public String getPayLoad(String token, String payLoadString) {
        if (StrUtil.isEmpty(token) || StrUtil.isEmpty(payLoadString)) {
            return null;
        }
        try {
            JWT jwt = JWTUtil.parseToken(token);
            return jwt.getPayload(payLoadString).toString();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean validateToken(String token) {
        try{
            boolean verify = JWTUtil.verify(token, jwtProperties.getSecret().getBytes());
            if (!verify) {
                return false;
            }
        } catch (Exception ex) {
            return false;
        }
        String payload = getPayLoad(token, JWT_PAYLOAD_EXPIRE_TIME);
        if (StrUtil.isEmpty(payload)) {
            return false;
        }
        long expireTimestamp = Long.parseLong(payload);
        return System.currentTimeMillis() <= expireTimestamp;
    }

    public Long getUserId(String token) {
        String payload = getPayLoad(token, JWT_PAYLOAD_USER_ID);
        if (StrUtil.isEmpty(payload)) {
            return null;
        }
        return Long.parseLong(payload);
    }

    public Long getRoleId(String token) {
        String payload = getPayLoad(token, JWT_PAYLOAD_ROLE_ID);
        if (StrUtil.isEmpty(payload)) {
            return null;
        }
        return Long.parseLong(payload);
    }

    public Long getTenantId(String token) {
        String payload = getPayLoad(token, JWT_PAYLOAD_TENANT_ID);
        if (StrUtil.isEmpty(payload)) {
            return null;
        }
        return Long.parseLong(payload);
    }

    public Long getExpireTime(String token) {
        String payload = getPayLoad(token, JWT_PAYLOAD_EXPIRE_TIME);
        if (StrUtil.isEmpty(payload)) {
            return null;
        }
        return Long.parseLong(payload);
    }

}
