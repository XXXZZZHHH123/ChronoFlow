package nus.edu.u.system.service.user;

import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.*;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.exception.ServiceException;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.dataobject.user.UserRoleDO;
import nus.edu.u.system.domain.dto.CreateUserDTO;
import nus.edu.u.system.domain.dto.RoleDTO;
import nus.edu.u.system.domain.dto.UpdateUserDTO;
import nus.edu.u.system.domain.dto.UserRoleDTO;
import nus.edu.u.system.domain.vo.user.BulkUpsertUsersRespVO;
import nus.edu.u.system.domain.vo.user.UserProfileRespVO;
import nus.edu.u.system.enums.user.UserStatusEnum;
import nus.edu.u.system.mapper.role.RoleMapper;
import nus.edu.u.system.mapper.user.UserMapper;
import nus.edu.u.system.mapper.user.UserRoleMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * User service implementation
 *
 * @author Lu Shuwen
 * @date 2025-08-30
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

  @Resource private UserMapper userMapper;

  @Resource private UserRoleMapper userRoleMapper;

  @Resource private RoleMapper roleMapper;

  @Resource private PasswordEncoder passwordEncoder;

  // Self-injection proxy to avoid transaction enhancement failure caused by internal calls of
  // similar methods
  @Resource @Lazy private UserService self;

  @Override
  public UserDO getUserByUsername(String username) {
    return userMapper.selectByUsername(username);
  }

  @Override
  public boolean isPasswordMatch(String rawPassword, String encodedPassword) {
    return passwordEncoder.matches(rawPassword, encodedPassword);
  }

  @Override
  public UserRoleDTO selectUserWithRole(Long userId) {
    return userMapper.selectUserWithRole(userId);
  }

    @Override
    public UserDO selectUserById(Long userId) {
        return userMapper.selectById(userId);
    }

  @Override
  @Transactional
  public Long createUserWithRoleIds(CreateUserDTO dto) {
    String email = dto.getEmail().trim();
    List<Long> roleIds = dto.getRoleIds().stream().distinct().toList();

    // 1) Email uniqueness check
    if (userMapper.existsEmail(email, null)) {
      throw exception(EMAIL_EXIST);
    }

    // 2) Check if the role exists
    int count = roleMapper.countByIds(roleIds);
    if (count != roleIds.size()) {
      throw exception(ROLE_NOT_FOUND);
    }

    // 3) Create a user
    UserDO user =
        UserDO.builder()
            .email(email)
            .remark(dto.getRemark())
            .status(UserStatusEnum.PENDING.getCode())
            .build();
    if (userMapper.insert(user) <= 0) {
      throw exception(USER_INSERT_FAILURE);
    }

    // 4) Bind the role
    if (!roleIds.isEmpty()) {
      for (Long roleId : roleIds) {
        UserRoleDO ur = UserRoleDO.builder().userId(user.getId()).roleId(roleId).build();

        int rows = userRoleMapper.insert(ur);
        if (rows <= 0) {
          throw exception(USER_ROLE_BIND_FAILURE);
        }
      }
    }

    return user.getId();
  }

  @Override
  @Transactional
  public UserDO updateUserWithRoleIds(UpdateUserDTO dto) {
    UserDO dbUser = userMapper.selectById(dto.getId());
    if (dbUser == null || Boolean.TRUE.equals(dbUser.getDeleted())) {
      throw exception(USER_NOT_FOUND);
    }

    // Email uniqueness check
    if (dto.getEmail() != null && userMapper.existsEmail(dto.getEmail(), dto.getId())) {
      throw exception(EMAIL_EXIST);
    }

    // Update user
    LambdaUpdateWrapper<UserDO> uw = Wrappers.<UserDO>lambdaUpdate().eq(UserDO::getId, dto.getId());
    if (dto.getEmail() != null) uw.set(UserDO::getEmail, dto.getEmail());
    if (dto.getRemark() != null) uw.set(UserDO::getRemark, dto.getRemark());
    if (userMapper.update(new UserDO(), uw) <= 0) {
      throw exception(UPDATE_FAILURE);
    }

    // Synchronize roles (null does not change; empty collection = clear)
    if (dto.getRoleIds() != null) {
      // Check if the role exists
      List<Long> targetList =
          dto.getRoleIds().stream().filter(Objects::nonNull).distinct().toList();
      if (!targetList.isEmpty()) {
        int n = roleMapper.countByIds(targetList);
        if (n != targetList.size()) throw exception(ROLE_NOT_FOUND);
      }
      syncUserRoles(dto.getId(), targetList);
    }
    return userMapper.selectById(dto.getId());
  }

  @Override
  @Transactional
  public void softDeleteUser(Long id) {
    UserDO db = userMapper.selectRawById(id);
    if (db == null) {
      throw exception(USER_NOTFOUND);
    }
    if (Boolean.TRUE.equals(db.getDeleted())) {
      throw exception(USER_ALREADY_DELETED);
    }
    int rows =
        userMapper.update(
            new UserDO(),
            Wrappers.<UserDO>lambdaUpdate()
                .set(UserDO::getDeleted, true)
                .eq(UserDO::getId, id)
                .eq(UserDO::getDeleted, false));
    if (rows <= 0) throw exception(UPDATE_FAILURE);
    userRoleMapper.delete(new LambdaQueryWrapper<UserRoleDO>().eq(UserRoleDO::getUserId, id));
  }

  @Override
  @Transactional
  public void restoreUser(Long id) {
    // 1) Check if the user exists and has been deleted
    UserDO db = userMapper.selectRawById(id);
    if (db == null) {
      throw exception(USER_NOTFOUND);
    }
    if (Boolean.FALSE.equals(db.getDeleted())) {
      // Not deleted, cannot be restored
      throw exception(USER_NOT_DELETED);
    }

    // 2) Restore deleted=0
    int rows =
        userMapper.update(
            new UserDO(),
            new LambdaUpdateWrapper<UserDO>().set(UserDO::getDeleted, 0).eq(UserDO::getId, id));
    if (rows <= 0) {
      throw exception(UPDATE_FAILURE);
    }
  }

  @Override
  @Transactional
  public void disableUser(Long id) {
    UserDO db = userMapper.selectById(id);
    if (db == null || Boolean.TRUE.equals(db.getDeleted())) {
      throw exception(USER_NOT_FOUND);
    }

    if (Objects.equals(db.getStatus(), UserStatusEnum.DISABLE.getCode())) {
      throw exception(USER_ALREADY_DISABLED);
    }

    int rows =
        userMapper.update(
            new UserDO(),
            new LambdaUpdateWrapper<UserDO>()
                .set(UserDO::getStatus, UserStatusEnum.DISABLE.getCode())
                .eq(UserDO::getId, id));
    if (rows <= 0) {
      throw exception(USER_DISABLE_FAILURE);
    }
  }

  @Override
  @Transactional
  public void enableUser(Long id) {
    UserDO db = userMapper.selectById(id);
    if (db == null || Boolean.TRUE.equals(db.getDeleted())) {
      throw exception(USER_NOT_FOUND);
    }

    if (Objects.equals(db.getStatus(), UserStatusEnum.ENABLE.getCode())) {
      throw exception(USER_ALREADY_ENABLED);
    }

    int rows =
        userMapper.update(
            new UserDO(),
            new LambdaUpdateWrapper<UserDO>()
                .set(UserDO::getStatus, UserStatusEnum.ENABLE.getCode())
                .eq(UserDO::getId, id));
    if (rows <= 0) {
      throw exception(USER_ENABLE_FAILURE);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserProfileRespVO> getAllUserProfiles() {
    List<UserRoleDTO> list = userMapper.selectAllUsersWithRoles();
    if (list.isEmpty()) {
      return Collections.emptyList();
    }

    return list.stream()
        .filter(
            dto -> !Objects.equals(dto.getUserId(), StpUtil.getLoginIdAsLong())) // Exclude myself
        .map(
            dto -> {
              UserProfileRespVO vo = new UserProfileRespVO();
              vo.setId(dto.getUserId());
              vo.setName(dto.getUsername());
              vo.setEmail(dto.getEmail());
              vo.setPhone(dto.getPhone());

              // (RoleDTO → id)
              List<Long> roleIds =
                  (dto.getRoles() == null)
                      ? Collections.emptyList()
                      : dto.getRoles().stream().map(RoleDTO::getId).toList();
              vo.setRoles(roleIds);

              // Registration status: status=2(PENDING) → Not registered; otherwise it is considered
              // registered
              boolean isRegistered =
                  !Objects.equals(dto.getStatus(), UserStatusEnum.PENDING.getCode());
              vo.setRegistered(isRegistered);

              return vo;
            })
        .toList();
  }

  //    @Override
  //    @Transactional
  //    public UserDO createUser(CreateProfileDTO dto) {
  //        if (dto.getPhone() != null && !ValidationUtils.isMobile(dto.getPhone())){
  //            throw exception(WRONG_MOBILE);
  //        }
  //        // Username/email/phone uniqueness check
  //        if (userMapper.existsUsername(dto.getUsername(), null)) {
  //            throw exception(USERNAME_EXIST);
  //        }
  //        if (userMapper.existsEmail(dto.getEmail(), null)) {
  //            throw exception(EMAIL_EXIST);
  //        }
  //        if (userMapper.existsPhone(dto.getPhone(), null)) {
  //            throw exception(PHONE_EXIST);
  //        }
  //
  //        UserDO user = UserDO.builder()
  //                .username(dto.getUsername())
  //                .password(passwordEncoder.encode(dto.getPassword()))
  //                .email(dto.getEmail())
  //                .phone(dto.getPhone())
  //                .remark(dto.getRemark())
  //                .status(UserStatusEnum.PENDING.getCode()) // 默认pending
  //                .build();
  //
  // user.setTenantId(Long.parseLong(StpUtil.getSession().get(SESSION_TENANT_ID).toString()));
  //        int rows = userMapper.insert(user);
  //        if (rows <= 0) {
  //            throw exception(USER_INSERT_FAILURE);
  //        }
  //        return user;
  //    }
  //
  //
  //    @Override
  //    @Transactional
  //    public UserDO updateUser(UpdateProfileDTO dto) {
  //        // 0) 预处理：trim，并把 "" 转成 null（表示“不更新该字段”）
  //        dto.setUsername(trimToNull(dto.getUsername()));
  //        dto.setPassword(trimToNull(dto.getPassword()));
  //        dto.setEmail(trimToNull(dto.getEmail()));
  //        dto.setPhone(trimToNull(dto.getPhone()));
  //        dto.setRemark(trimToNull(dto.getRemark()));
  //
  //        // 1) 存在性校验
  //        UserDO db = userMapper.selectById(dto.getId());
  //        if (db == null) {
  //            throw exception(USER_NOTFOUND);
  //        }
  //
  //        // 2) 可选字段的业务校验
  //        if (dto.getPhone() != null && !ValidationUtils.isMobile(dto.getPhone())) {
  //            throw exception(WRONG_MOBILE);
  //        }
  //
  //        // 3) 业务唯一性校验（排除自己）
  //        if (dto.getUsername() != null && userMapper.existsUsername(dto.getUsername(),
  // dto.getId())) {
  //            throw exception(USERNAME_EXIST);
  //        }
  //        if (dto.getEmail() != null && userMapper.existsEmail(dto.getEmail(), dto.getId())) {
  //            throw exception(EMAIL_EXIST);
  //        }
  //        if (dto.getPhone() != null && userMapper.existsPhone(dto.getPhone(), dto.getId())) {
  //            throw exception(PHONE_EXIST);
  //        }
  //
  //        // 4) 只更新非空字段
  //        LambdaUpdateWrapper<UserDO> uw = Wrappers.<UserDO>lambdaUpdate()
  //                .eq(UserDO::getId, dto.getId());
  //
  //        boolean hasUpdate = false;
  //        if (dto.getUsername() != null) { uw.set(UserDO::getUsername, dto.getUsername());
  // hasUpdate = true; }
  //        if (dto.getEmail() != null)    { uw.set(UserDO::getEmail, dto.getEmail());
  // hasUpdate = true; }
  //        if (dto.getPhone() != null)    { uw.set(UserDO::getPhone, dto.getPhone());
  // hasUpdate = true; }
  //        if (dto.getRemark() != null)   { uw.set(UserDO::getRemark, dto.getRemark());
  // hasUpdate = true; }
  //        if (dto.getPassword() != null) {
  //            uw.set(UserDO::getPassword, passwordEncoder.encode(dto.getPassword()));
  //            hasUpdate = true;
  //        }
  //
  //        // 若没有任何可更新字段，直接返回当前数据，避免无意义的 UPDATE
  //        if (hasUpdate) {
  //            if (userMapper.update(new UserDO(), uw) <= 0) {
  //                throw exception(UPDATE_FAILURE);
  //            }
  //        }
  //
  //        // 5) 返回最新数据
  //        return userMapper.selectById(dto.getId());
  //    }

  /** Core: add only/delete only/revive */
  private void syncUserRoles(Long userId, List<Long> targetList) {
    Set<Long> current = new HashSet<>(userRoleMapper.selectAliveRoleIdsByUser(userId));
    Set<Long> target = new HashSet<>(Optional.ofNullable(targetList).orElseGet(List::of));

    Set<Long> toRemove = new HashSet<>(current);
    toRemove.removeAll(target);
    Set<Long> toAdd = new HashSet<>(target);
    toAdd.removeAll(current);

    // 1) 逻辑删除多余
    if (!toRemove.isEmpty()) {
      userRoleMapper.batchLogicalDelete(userId, toRemove);
    }

    if (!toAdd.isEmpty()) {
      // 2) 先复活历史记录（deleted = true 的那批）
      userRoleMapper.batchRevive(userId, toAdd);

      // 3) 再只插入仍不存在的（避免唯一索引冲突）
      userRoleMapper.insertMissing(userId, toAdd);
    }
  }

  @Override
  public BulkUpsertUsersRespVO bulkUpsertUsers(List<CreateUserDTO> rawRows) {
    if (rawRows == null || rawRows.isEmpty()) {
      return BulkUpsertUsersRespVO.builder()
          .totalRows(0)
          .createdCount(0)
          .updatedCount(0)
          .failedCount(0)
          .failures(Collections.emptyList())
          .build();
    }

    // 0) Pre-cleaning: deduplication by email (keep the first one), deduplication by role
    Map<String, CreateUserDTO> byEmail = new LinkedHashMap<>();
    for (CreateUserDTO r : rawRows) {
      if (r == null || r.getEmail() == null) continue;
      String email = normalizeEmail(r.getEmail());
      if (email.isEmpty()) continue;

      List<Long> roles =
          Optional.ofNullable(r.getRoleIds()).orElse(Collections.emptyList()).stream()
              .filter(Objects::nonNull)
              .distinct()
              .toList();

      r.setEmail(email);
      r.setRoleIds(roles);
      byEmail.putIfAbsent(email, r);
    }
    List<CreateUserDTO> rows = new ArrayList<>(byEmail.values());
    int total = rows.size();

    int created = 0, updated = 0;
    List<BulkUpsertUsersRespVO.RowFailure> failures = new ArrayList<>();

    // 1) Check the DB for existing mailboxes at once and decide whether to create or update
    // (optional optimization)
    List<String> emails = rows.stream().map(CreateUserDTO::getEmail).toList();
    Set<String> exists = new HashSet<>(userMapper.selectExistingEmails(emails));

    for (CreateUserDTO r : rows) {
      int rowIndex = r.getRowIndex() != null ? r.getRowIndex() : 0;
      try {
        // Use proxy (self) to ensure @Transactional(REQUIRES_NEW) takes effect
        boolean isCreated = self.processSingleRowWithNewTx(r, exists.contains(r.getEmail()));
        if (isCreated) created++;
        else updated++;
      } catch (ServiceException e) {
        failures.add(
            BulkUpsertUsersRespVO.RowFailure.builder()
                .rowIndex(rowIndex)
                .email(r.getEmail())
                .reason(e.getMessage())
                .build());
      } catch (Exception e) {
        failures.add(
            BulkUpsertUsersRespVO.RowFailure.builder()
                .rowIndex(rowIndex)
                .email(r.getEmail())
                .reason("INTERNAL_ERROR: " + e.getClass().getSimpleName())
                .build());
      }
    }

    return BulkUpsertUsersRespVO.builder()
        .totalRows(total)
        .createdCount(created)
        .updatedCount(updated)
        .failedCount(failures.size())
        .failures(failures)
        .build();
  }

  /** Single-row transaction: new transaction. Returns true if created; false if updated. */
  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean processSingleRowWithNewTx(CreateUserDTO row, boolean dbExists) {
    if (dbExists) {
      // Update: Use email to find id, then reuse your update method
      Long userId = userMapper.selectIdByEmail(row.getEmail());
      UpdateUserDTO u =
          UpdateUserDTO.builder()
              .id(userId)
              // The mailbox is usually not changed; if you want to change it, put null here to "do
              // not change the mailbox"
              .email(null)
              .remark(row.getRemark())
              .roleIds(row.getRoleIds())
              .build();
      updateUserWithRoleIds(
          u); // Your existing update logic (including role difference synchronization)
      return false;
    } else {
      // Create: reuse your existing single creation
      createUserWithRoleIds(row);
      return true;
    }
  }

  @Override
  public List<Long> getAliveRoleIdsByUserId(Long userId) {
    return userRoleMapper.selectRoleIdsByUserId(userId);
  }

  private String normalizeEmail(String email) {
    return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
  }
}
