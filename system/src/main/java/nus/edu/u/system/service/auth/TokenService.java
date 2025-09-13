package nus.edu.u.system.service.auth;

import nus.edu.u.system.domain.dto.TokenDTO;
import nus.edu.u.system.domain.dto.UserTokenDTO;

/**
 * Token service interface
 *
 * @author Lu Shuwen
 * @date 2025-08-30
 */
public interface TokenService {

    /**
     * Create a refresh token and store in redis
     *
     * @param userTokenDTO parameters to create token
     * @return token
     */
    String createRefreshToken(UserTokenDTO userTokenDTO);

    /**
     * Remove token from redis
     *
     * @param token token
     */
    void removeToken(String token);

    /**
     * Get userId in refresh token from redis
     */
    Long getUserIdFromRefreshToken(String refreshToken);
}
