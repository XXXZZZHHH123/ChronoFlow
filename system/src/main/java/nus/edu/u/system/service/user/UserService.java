package nus.edu.u.system.service.user;

import java.util.List;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.dto.*;
import nus.edu.u.system.domain.vo.user.BulkUpsertUsersRespVO;
import nus.edu.u.system.domain.vo.user.UserProfileRespVO;

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
   * @param rawPassword Unencrypted password
   * @param encodedPassword Encrypted password
   * @return Match result
   */
  boolean isPasswordMatch(String rawPassword, String encodedPassword);

    /**
     * Select user and his role by userId
     * @param userId user id
     * @return UserRoleDTO
     */
    UserRoleDTO selectUserWithRole(Long userId);

    /**
     * Select user by id
     * @param userId
     * @return UserDO
     */
    UserDO selectUserById(Long userId);
  /**
   * Select user and his role by userId
   *
   * @param userId user id
   * @return UserRoleDTO
   */
  UserRoleDTO selectUserWithRole(Long userId);

  Long createUserWithRoleIds(CreateUserDTO dto);

  UserDO updateUserWithRoleIds(UpdateUserDTO dto);

  //    UserDO createUser(CreateProfileDTO dto);
  //    UserDO updateUser(UpdateProfileDTO dto);

  void softDeleteUser(Long userId);

  void restoreUser(Long id);

  void disableUser(Long id);

  void enableUser(Long id);

  List<UserProfileRespVO> getAllUserProfiles();

  BulkUpsertUsersRespVO bulkUpsertUsers(List<CreateUserDTO> rawRows);

  boolean processSingleRowWithNewTx(CreateUserDTO row, boolean dbExists);

  List<Long> getAliveRoleIdsByUserId(Long userId);
}
