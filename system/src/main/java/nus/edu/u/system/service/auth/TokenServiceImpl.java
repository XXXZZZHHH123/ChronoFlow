package nus.edu.u.system.service.auth;

import static nus.edu.u.common.constant.CacheConstants.LOGIN_REFRESH_TOKEN_KEY;
import static nus.edu.u.common.exception.enums.GlobalErrorCodeConstants.EXPIRED_LOGIN_CREDENTIALS;
import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.framework.security.config.SecurityProperties;
import nus.edu.u.system.domain.dto.UserTokenDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Token service implementation
 *
 * @author Lu Shuwen
 * @date 2025-08-30
 */
@Service
@Slf4j
public class TokenServiceImpl implements TokenService {

  @Resource private StringRedisTemplate stringRedisTemplate;

  @Resource private SecurityProperties securityProperties;

  @Override
  public String createRefreshToken(UserTokenDTO userTokenDTO) {
    // 1.Use utils to create token
    String token = UUID.randomUUID().toString();
    if (userTokenDTO.isRemember()) {
      // 2.Put token into redis
      stringRedisTemplate
          .opsForValue()
          .set(
              LOGIN_REFRESH_TOKEN_KEY + token,
              userTokenDTO.getId().toString(),
              securityProperties.getRefreshTokenExpire(),
              TimeUnit.SECONDS);
    }
    // 3.Return token
    return token;
  }

  @Override
  public void removeToken(String token) {
    try {
      // Remove token from redis
      stringRedisTemplate.delete(LOGIN_REFRESH_TOKEN_KEY + token);
    } catch (Exception e) {
      throw exception(EXPIRED_LOGIN_CREDENTIALS);
    }
    StpUtil.logout();
  }

  @Override
  public Long getUserIdFromRefreshToken(String refreshToken) {
    String userIdStr =
        stringRedisTemplate.opsForValue().get(LOGIN_REFRESH_TOKEN_KEY + refreshToken);
    if (StrUtil.isNotEmpty(userIdStr)) {
      return Long.parseLong(userIdStr);
    }
    return null;
  }
}
