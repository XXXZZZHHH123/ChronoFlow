package nus.edu.u.system.service.auth;

import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.vo.auth.LoginReqVO;
import nus.edu.u.system.domain.vo.auth.LoginRespVO;

/**
 * Authentication service interface
 *
 * @author Lu Shuwen
 * @date 2025-08-30
 */
public interface AuthService {

    /**
     * Select by username and match password If matched, return a UserDO object If not matched, throw
     * exception
     *
     * @param username name
     * @param password psd
     * @return UserDO
     */
    UserDO authenticate(String username, String password);

    /**
     * Login service
     *
     * @param reqVO LoginReqVO
     * @return LoginRespVO
     */
    LoginRespVO login(LoginReqVO reqVO);

    /**
     * Logout
     *
     * @param token access token
     */
    void logout(String token);

    LoginRespVO refresh(String refreshTokenVO);
}
