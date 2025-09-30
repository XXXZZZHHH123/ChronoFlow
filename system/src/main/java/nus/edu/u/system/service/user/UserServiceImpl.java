package nus.edu.u.system.service.user;

import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.*;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import java.util.*;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.enums.CommonStatusEnum;
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

    private static final Set<Long> FORBIDDEN_ROLE_IDS = Set.of(1L);

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
        // Filter out roleId=1 (admin) and remove duplicates
        List<Long> roleIds = dto.getRoleIds().stream().distinct().toList();

        if (roleIds.stream().anyMatch(FORBIDDEN_ROLE_IDS::contains)) {
            throw exception(ROLE_NOT_FOUND);
        }

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
        LambdaUpdateWrapper<UserDO> uw =
                Wrappers.<UserDO>lambdaUpdate().eq(UserDO::getId, dto.getId());
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
            if (targetList.stream().anyMatch(FORBIDDEN_ROLE_IDS::contains)) {
                throw exception(ROLE_NOT_FOUND);
            }
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
                        new LambdaUpdateWrapper<UserDO>()
                                .set(UserDO::getDeleted, 0)
                                .eq(UserDO::getId, id));
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
                        dto ->
                                !Objects.equals(
                                        dto.getUserId(),
                                        StpUtil.getLoginIdAsLong())) // Exclude myself
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

                            // Registration status: status=2(PENDING) → Not registered; otherwise it
                            // is considered
                            // registered
                            boolean isRegistered =
                                    !Objects.equals(
                                            dto.getStatus(), UserStatusEnum.PENDING.getCode());
                            vo.setRegistered(isRegistered);

                            return vo;
                        })
                .toList();
    }

    /** Core: add only/delete only/revive */
    private void syncUserRoles(Long userId, List<Long> targetList) {
        Set<Long> current = new HashSet<>(userRoleMapper.selectAliveRoleIdsByUser(userId));
        Set<Long> target = new HashSet<>(Optional.ofNullable(targetList).orElseGet(List::of));

        Set<Long> toRemove = new HashSet<>(current);
        toRemove.removeAll(target);
        Set<Long> toAdd = new HashSet<>(target);
        toAdd.removeAll(current);

        // 1) Logic delete redundant
        if (!toRemove.isEmpty()) {
            userRoleMapper.batchLogicalDelete(userId, toRemove);
        }

        if (!toAdd.isEmpty()) {
            // 2) Resurrect the historical records first (those with deleted = true)）
            userRoleMapper.batchRevive(userId, toAdd);

            // 3) Then only insert those that do not exist yet (to avoid unique index conflicts）
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

        int created = 0, updated = 0;
        List<BulkUpsertUsersRespVO.RowFailure> failures = new ArrayList<>();

        for (CreateUserDTO raw : rawRows) {
            int rowIndex = raw.getRowIndex() != null ? raw.getRowIndex() : 0;
            String rawEmail = raw.getEmail();

            try {
                validateCreateArgs(rawEmail, raw.getRoleIds());

                String email = normalizeEmail(rawEmail);
                List<Long> roleIds = normalizeAndCheckRoles(raw.getRoleIds());
                String remark = raw.getRemark();

                boolean isCreated = self.tryCreateOrFallbackToUpdate(email, remark, roleIds);
                if (isCreated) created++;
                else updated++;

            } catch (ServiceException e) {
                failures.add(
                        BulkUpsertUsersRespVO.RowFailure.builder()
                                .rowIndex(rowIndex)
                                .email(rawEmail)
                                .reason(e.getMessage())
                                .build());
            } catch (Exception e) {
                failures.add(
                        BulkUpsertUsersRespVO.RowFailure.builder()
                                .rowIndex(rowIndex)
                                .email(rawEmail)
                                .reason("INTERNAL_ERROR: " + e.getClass().getSimpleName())
                                .build());
            }
        }

        return BulkUpsertUsersRespVO.builder()
                .totalRows(rawRows.size())
                .createdCount(created)
                .updatedCount(updated)
                .failedCount(failures.size())
                .failures(failures)
                .build();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryCreateOrFallbackToUpdate(String email, String remark, List<Long> roleIds) {
        try {
            // A) 先尝试创建
            CreateUserDTO dto =
                    CreateUserDTO.builder().email(email).remark(remark).roleIds(roleIds).build();
            createUserWithRoleIds(dto);
            return true;

        } catch (ServiceException ex) {
            if (Objects.equals(ex.getCode(), EMAIL_EXIST.getCode())) {
                Long userId = userMapper.selectIdByEmail(email);
                if (userId == null) {
                    throw new ServiceException(NULL_USERID);
                }

                validateUpdateArgs(roleIds);

                UpdateUserDTO u =
                        UpdateUserDTO.builder()
                                .id(userId)
                                .email(null)
                                .remark(remark)
                                .roleIds(roleIds)
                                .build();

                updateUserWithRoleIds(u);
                return false;
            }
            // Other business exceptions (such as ROLE_NOT_FOUND) continue to be thrown to the outer
            // layer
            // to record failure
            throw ex;
        }
    }

    @Override
    public List<Long> getAliveRoleIdsByUserId(Long userId) {
        return userRoleMapper.selectRoleIdsByUserId(userId);
    }

    @Override
    public List<UserProfileRespVO> getEnabledUserProfiles() {
        List<UserRoleDTO> list = userMapper.selectAllUsersWithRoles();
        if (list.isEmpty()) {
            return Collections.emptyList();
        }

        return list.stream()
                .filter(dto ->
                        !Objects.equals(dto.getUserId(), StpUtil.getLoginIdAsLong())
                                && Objects.equals(dto.getStatus(), CommonStatusEnum.ENABLE.getStatus())
                )
                .map(this::convertToUserProfileRespVO)
                .toList();
    }

    // 提取的私有方法
    private UserProfileRespVO convertToUserProfileRespVO(UserRoleDTO dto) {
        UserProfileRespVO vo = new UserProfileRespVO();
        vo.setId(dto.getUserId());
        vo.setName(dto.getUsername());
        vo.setEmail(dto.getEmail());
        vo.setPhone(dto.getPhone());

        List<Long> roleIds =
                (dto.getRoles() == null)
                        ? Collections.emptyList()
                        : dto.getRoles().stream().map(RoleDTO::getId).toList();
        vo.setRoles(roleIds);

        boolean isRegistered =
                !Objects.equals(dto.getStatus(), UserStatusEnum.PENDING.getCode());
        vo.setRegistered(isRegistered);

        return vo;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    private void validateCreateArgs(String rawEmail, List<Long> rawRoleIds) {
        String email = normalizeEmail(rawEmail);
        if (email.isBlank()) {
            throw new ServiceException(EMAIL_BLANK);
        }
        if (!isValidEmail(email)) {
            throw new ServiceException(INVALID_EMAIL);
        }
        if (rawRoleIds == null || rawRoleIds.isEmpty()) {
            throw new ServiceException(EMPTY_ROLEIDS);
        }
    }

    private void validateUpdateArgs(List<Long> rawRoleIds) {
        if (rawRoleIds == null || rawRoleIds.isEmpty()) {
            throw new ServiceException(EMPTY_ROLEIDS);
        }
    }

    // Role general business verification (prohibit roles + deduplication normalization)
    private List<Long> normalizeAndCheckRoles(List<Long> rawRoleIds) {
        List<Long> roleIds = rawRoleIds.stream().filter(Objects::nonNull).distinct().toList();

        if (roleIds.stream().anyMatch(FORBIDDEN_ROLE_IDS::contains)) {
            throw exception(ROLE_NOT_FOUND);
        }
        return roleIds;
    }


}
