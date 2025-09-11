package nus.edu.u.system.service.user;

import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.dto.UserCreateDTO;

/**
 * User service interface
 *
 * @author Lu Shuwen
 * @date 2025-08-30
 */
public interface UserService {
    /**
     * Select one UserDO object from db
     *
     * @param username name
     * @return UserDO
     */
    UserDO getUserByUsername(String username);

    /**
     * Check if two password are matched
     *
     * @param rawPassword   Unencrypted password
     * @param encodedPassword   Encrypted password
     * @return  Match result
     */
    boolean isPasswordMatch(String rawPassword, String encodedPassword);

    Long createUser(UserCreateDTO dto);
}
