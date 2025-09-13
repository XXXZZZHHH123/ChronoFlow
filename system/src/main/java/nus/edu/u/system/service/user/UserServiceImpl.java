package nus.edu.u.system.service.user;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.common.utils.validation.ValidationUtils;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.dataobject.user.UserRoleDO;
import nus.edu.u.system.domain.dto.*;
import nus.edu.u.system.domain.vo.user.UserProfileRespVO;
import nus.edu.u.system.enums.user.UserStatusEnum;
import nus.edu.u.system.mapper.role.RoleMapper;
import nus.edu.u.system.mapper.user.UserMapper;
import nus.edu.u.system.mapper.user.UserRoleMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static nus.edu.u.common.constant.Constants.SESSION_TENANT_ID;
import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.*;
/**
 * User service implementation
 *
 * @author Lu Shuwen
 * @date 2025-08-30
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService{

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

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
    @Transactional(readOnly = true)
    public List<UserProfileRespVO> getAllUserProfiles() {
        List<UserRoleDTO> list = userMapper.selectAllUsersWithRoles();
        if (list.isEmpty()) {
            return Collections.emptyList();
        }

        return list.stream().map(dto -> {
            UserProfileRespVO vo = new UserProfileRespVO();
            vo.setId(dto.getUserId());
            vo.setName(dto.getUsername());
            vo.setEmail(dto.getEmail());
            vo.setPhone(dto.getPhone());

            // 转换 roles (RoleDTO → id)
            List<Long> roleIds = (dto.getRoles() == null)
                    ? Collections.emptyList()
                    : dto.getRoles().stream()
                    .map(RoleDTO::getId)
                    .toList();
            vo.setRoles(roleIds);

            // 注册状态：status=2(PENDING) → 未注册；否则算已注册
            boolean isRegistered = !Objects.equals(dto.getStatus(), UserStatusEnum.PENDING.getCode());
            vo.setRegistered(isRegistered);

            return vo;
        }).toList();
    }


    @Override
    @Transactional
    public Long createUserWithRoleIds(OrganizerCreateUserDTO dto) {
        String email = dto.getEmail().trim();
        List<Long> roleIds = dto.getRoleIds().stream().distinct().toList();

        // 1) 邮箱唯一性校验
        if (userMapper.existsEmail(email, null)) {
            throw exception(EMAIL_EXIST);
        }

        // 2) 校验角色是否存在
        int count = roleMapper.countByIds(roleIds);
        if (count != roleIds.size()) {
            throw exception(ROLE_NOT_FOUND);
        }

        // 3) 创建用户
        UserDO user = UserDO.builder()
                .email(email)
                .remark(dto.getRemark())
                .status(UserStatusEnum.PENDING.getCode())
                .build();
        user.setTenantId(Long.parseLong(StpUtil.getSession().get(SESSION_TENANT_ID).toString()));

        if (userMapper.insert(user) <= 0) {
            throw exception(USER_INSERT_FAILURE);
        }


        // 4) 绑定角色
        if (!roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                UserRoleDO ur = UserRoleDO.builder()
                        .userId(user.getId())
                        .roleId(roleId)
                        .build();

                int rows = userRoleMapper.insert(ur);
                if (rows <= 0) {
                    throw exception(USER_ROLE_BIND_FAILURE); // 插入失败时抛出异常
                }
            }
        }

        return user.getId();
    }


    @Override
    @Transactional
    public UserDO updateUserWithRoleIds(OrganizerUpdateUserDTO dto) {
        UserDO dbUser = userMapper.selectById(dto.getId());
        if (dbUser == null || Boolean.TRUE.equals(dbUser.getDeleted())) {
            throw exception(USER_NOT_FOUND);
        }

        // 邮箱唯一性校验
        if (dto.getEmail() != null && userMapper.existsEmail(dto.getEmail(), dto.getId())) {
            throw exception(EMAIL_EXIST);
        }


        // 更新用户
        LambdaUpdateWrapper<UserDO> uw = Wrappers.<UserDO>lambdaUpdate()
                .eq(UserDO::getId, dto.getId());
        if (dto.getEmail() != null) uw.set(UserDO::getEmail, dto.getEmail());
        if (dto.getRemark() != null) uw.set(UserDO::getRemark, dto.getRemark());
        if (userMapper.update(new UserDO(), uw) <= 0) {
            throw exception(UPDATE_FAILURE);
        }

        // 同步角色（null 不改；空集合 = 清空）
        if (dto.getRoleIds() != null) {
            // 校验角色存在性
            List<Long> targetList = dto.getRoleIds().stream()
                    .filter(Objects::nonNull).distinct().toList();
            if (!targetList.isEmpty()) {
                int n = roleMapper.countByIds(targetList);
                if (n != targetList.size()) throw exception(ROLE_NOT_FOUND);
            }
            syncUserRoles(dto.getId(), targetList);
        }

        // 返回最新用户信息
        return userMapper.selectById(dto.getId());
    }


    @Override
    @Transactional
    public UserDO createUser(UserCreateDTO dto) {
        if (dto.getPhone() != null && !ValidationUtils.isMobile(dto.getPhone())){
            throw exception(WRONG_MOBILE);
        }
        // Username/email/phone uniqueness check
        if (userMapper.existsUsername(dto.getUsername(), null)) {
            throw exception(USERNAME_EXIST);
        }
        if (userMapper.existsEmail(dto.getEmail(), null)) {
            throw exception(EMAIL_EXIST);
        }
        if (userMapper.existsPhone(dto.getPhone(), null)) {
            throw exception(PHONE_EXIST);
        }

            UserDO user = UserDO.builder()
                    .username(dto.getUsername())
                    .password(passwordEncoder.encode(dto.getPassword()))
                    .email(dto.getEmail())
                    .phone(dto.getPhone())
                    .remark(dto.getRemark())
                    .status(UserStatusEnum.PENDING.getCode()) // 默认pending
                    .build();
            user.setTenantId(Long.parseLong(StpUtil.getSession().get(SESSION_TENANT_ID).toString()));
            int rows = userMapper.insert(user);
            if (rows <= 0) {
                throw exception(USER_INSERT_FAILURE);
            }
            return user;
        }


    @Override
    @Transactional
    public UserDO updateUser(UserUpdateDTO dto) {
        // 0) 预处理：trim，并把 "" 转成 null（表示“不更新该字段”）
        dto.setUsername(trimToNull(dto.getUsername()));
        dto.setPassword(trimToNull(dto.getPassword()));
        dto.setEmail(trimToNull(dto.getEmail()));
        dto.setPhone(trimToNull(dto.getPhone()));
        dto.setRemark(trimToNull(dto.getRemark()));

        // 1) 存在性校验
        UserDO db = userMapper.selectById(dto.getId());
        if (db == null) {
            throw exception(USER_NOTFOUND);
        }

        // 2) 可选字段的业务校验
        if (dto.getPhone() != null && !ValidationUtils.isMobile(dto.getPhone())) {
            throw exception(WRONG_MOBILE);
        }

        // 3) 业务唯一性校验（排除自己）
        if (dto.getUsername() != null && userMapper.existsUsername(dto.getUsername(), dto.getId())) {
            throw exception(USERNAME_EXIST);
        }
        if (dto.getEmail() != null && userMapper.existsEmail(dto.getEmail(), dto.getId())) {
            throw exception(EMAIL_EXIST);
        }
        if (dto.getPhone() != null && userMapper.existsPhone(dto.getPhone(), dto.getId())) {
            throw exception(PHONE_EXIST);
        }

        // 4) 只更新非空字段
        LambdaUpdateWrapper<UserDO> uw = Wrappers.<UserDO>lambdaUpdate()
                .eq(UserDO::getId, dto.getId());

        boolean hasUpdate = false;
        if (dto.getUsername() != null) { uw.set(UserDO::getUsername, dto.getUsername()); hasUpdate = true; }
        if (dto.getEmail() != null)    { uw.set(UserDO::getEmail, dto.getEmail());       hasUpdate = true; }
        if (dto.getPhone() != null)    { uw.set(UserDO::getPhone, dto.getPhone());       hasUpdate = true; }
        if (dto.getRemark() != null)   { uw.set(UserDO::getRemark, dto.getRemark());     hasUpdate = true; }
        if (dto.getPassword() != null) {
            uw.set(UserDO::getPassword, passwordEncoder.encode(dto.getPassword()));
            hasUpdate = true;
        }

        // 若没有任何可更新字段，直接返回当前数据，避免无意义的 UPDATE
        if (hasUpdate) {
            if (userMapper.update(new UserDO(), uw) <= 0) {
                throw exception(UPDATE_FAILURE);
            }
        }

        // 5) 返回最新数据
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
            // 已是删除状态，按需抛错或直接返回
            throw exception(USER_ALREADY_DELETED);
        }
        int rows = userMapper.update(new UserDO(),
                Wrappers.<UserDO>lambdaUpdate()
                        .set(UserDO::getDeleted, true)
                        .eq(UserDO::getId, id)
                        .eq(UserDO::getDeleted, false));
        if (rows <= 0) throw exception(UPDATE_FAILURE);
    }

    @Override
    @Transactional
    public void restoreUser(Long id) {
        // 1) 查用户是否存在且已删除
        UserDO db = userMapper.selectRawById(id);
        if (db == null) {
            throw exception(USER_NOTFOUND);
        }
        if (Boolean.FALSE.equals(db.getDeleted())) {
            // 未删除，无法恢复
            throw exception(USER_NOT_DELETED);
        }

        // 2) 恢复 deleted=0
        int rows = userMapper.restoreRawById(id, StpUtil.getLoginIdAsString());
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
            // 已经是禁用状态
            throw exception(USER_ALREADY_DISABLED);
        }

        int rows = userMapper.updateUserStatus(id, UserStatusEnum.DISABLE.getCode());
        if (rows <= 0) {
            throw exception(UPDATE_FAILURE);
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

        int rows = userMapper.updateUserStatus(id, UserStatusEnum.ENABLE.getCode());
        if (rows <= 0) {
            throw exception(UPDATE_FAILURE);
        }
    }


    /** 工具：去空白；空串 => null */
    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }


    /** 核心：只增/只删/复活 */
    private void syncUserRoles(Long userId, List<Long> targetList) {
        // 当前有效角色
        Set<Long> current = new HashSet<>(userRoleMapper.selectAliveRoleIdsByUser(userId));
        Set<Long> target  = new HashSet<>(targetList);

        Set<Long> toRemove = new HashSet<>(current); toRemove.removeAll(target);
        Set<Long> toAdd    = new HashSet<>(target);  toAdd.removeAll(current);

        String operator = StpUtil.getLoginIdAsString();
        Long tenantId = Long.valueOf(StpUtil.getSession().get(SESSION_TENANT_ID).toString());    // 当前租户
        // 2) 逻辑删除
        if (!toRemove.isEmpty()) {
            userRoleMapper.batchLogicalDelete(userId, toRemove, operator);
        }

        // 3) 复活 + Upsert
        if (!toAdd.isEmpty()) {
            userRoleMapper.batchRevive(userId, toAdd, operator);

            // 构造每条记录，预生成 id（MP 雪花）
            List<UserRoleDO> records = toAdd.stream().map(rid -> UserRoleDO.builder()
                    .id(IdWorker.getId())    // 关键：MP 生成主键
                    .userId(userId)
                    .roleId(rid)
                    .tenantId(tenantId)
                    .creator(operator)
                    .updater(operator)
                    .deleted(false)
                    .build()).toList();

            userRoleMapper.batchUpsertUserRoles(records);
        }
    }

}
