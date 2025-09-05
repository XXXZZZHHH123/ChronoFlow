package nus.edu.u.system.service.auth;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.config.JwtProperties;
import nus.edu.u.common.utils.security.JwtUtils;
import nus.edu.u.system.domain.dto.TokenDTO;
import nus.edu.u.system.domain.dto.UserTokenDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static nus.edu.u.common.constant.CacheConstants.LOGIN_ACCESS_TOKEN_KEY;
import static nus.edu.u.common.constant.CacheConstants.LOGIN_REFRESH_TOKEN_KEY;

/**
 * Token service implementation
 *
 * @author Lu Shuwen
 * @date 2025-08-30
 */
@Service
@Slf4j
public class TokenServiceImpl implements TokenService {

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private JwtProperties jwtProperties;

    @Override
    public TokenDTO createAccessToken(UserTokenDTO userTokenDTO) {
        // 1.Use utils to create token
        String token = jwtUtils.generateAccessToken(userTokenDTO.getId(), userTokenDTO.getRoleId(), userTokenDTO.getTenantId());
        // 2.Put token into redis
        stringRedisTemplate.opsForValue().set(LOGIN_ACCESS_TOKEN_KEY + userTokenDTO.getId(), token,
                jwtProperties.getAccessExpire(), TimeUnit.SECONDS);
        // 3.Build response object
        return TokenDTO.builder()
                .accessToken(token)
                .accessTokenExpireTime(System.currentTimeMillis() + jwtProperties.getAccessExpire() * 1000)
                .build();
    }

    @Override
    public String createRefreshToken(UserTokenDTO userTokenDTO) {
        // 1.Use utils to create token
        String token = jwtUtils.generateRefreshToken(userTokenDTO.getId(), userTokenDTO.getRoleId(), userTokenDTO.getTenantId());
        if (userTokenDTO.isRemember()) {
            // 2.Put token into redis
            stringRedisTemplate.opsForValue().set(LOGIN_REFRESH_TOKEN_KEY + userTokenDTO.getId(), token,
                    jwtProperties.getRefreshExpire(), TimeUnit.SECONDS);
        }
        // 3.Return token
        return token;
    }

    @Override
    public void removeToken(String token) {
        Long userId = jwtUtils.getUserId(token);
        if (userId != null) {
            // Remove token from redis
            stringRedisTemplate.delete(LOGIN_ACCESS_TOKEN_KEY + userId);
            stringRedisTemplate.delete(LOGIN_REFRESH_TOKEN_KEY + userId);
        }
    }

    @Override
    public TokenDTO refreshToken(String refreshToken) {
        // 1.Verify token
        if (!jwtUtils.validateToken(refreshToken)) {
            return null;
        }
        Long userId = jwtUtils.getUserId(refreshToken);
        Long roleId = jwtUtils.getRoleId(refreshToken);
        Long tenantId = jwtUtils.getTenantId(refreshToken);
        // 2.Check if refresh token is in redis
        boolean hasKey = stringRedisTemplate.hasKey(LOGIN_REFRESH_TOKEN_KEY + userId);
        if (!hasKey) {
            return null;
        }
        // 3.Create access token
        String accessToken = jwtUtils.generateAccessToken(userId, roleId, tenantId);
        // 4.Add access token in redis
        stringRedisTemplate.opsForValue().set(LOGIN_ACCESS_TOKEN_KEY + userId, accessToken,
                jwtProperties.getAccessExpire(), TimeUnit.SECONDS);
        // 5.Build return object
        return TokenDTO.builder()
                .accessToken(accessToken)
                .accessTokenExpireTime(System.currentTimeMillis() + jwtProperties.getAccessExpire() * 1000)
                .userId(userId)
                .build();
    }
}
