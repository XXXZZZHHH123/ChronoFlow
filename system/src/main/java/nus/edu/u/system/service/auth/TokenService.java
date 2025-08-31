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
     * Create an access token and store in redis
     *
     * @param userTokenDTO parameters to create token
     * @return token
     */
    TokenDTO createAccessToken(UserTokenDTO userTokenDTO);

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
     * Refresh access token
     *
     * @param refreshToken refresh token
     * @return TokenDTO
     */
    TokenDTO refreshToken(String refreshToken);

}
