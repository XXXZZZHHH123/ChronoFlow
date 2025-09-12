package nus.edu.u.system.service.user;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.common.utils.validation.ValidationUtils;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.dto.UserCreateDTO;
import nus.edu.u.system.domain.dto.UserRoleDTO;
import nus.edu.u.system.domain.dto.UserUpdateDTO;
import nus.edu.u.system.mapper.user.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public UserDO createUser(UserCreateDTO dto) {
        if (dto.getPhone() != null && !ValidationUtils.isMobile(dto.getPhone())){
            throw exception(WRONG_MOBILE);
        }
        // Username/email uniqueness check
        if (userMapper.selectCount(Wrappers.<UserDO>lambdaQuery()
                .eq(UserDO::getUsername, dto.getUsername())) > 0) {
            throw exception(USERNAME_EXIST);
        }
        if (userMapper.selectCount(Wrappers.<UserDO>lambdaQuery()
                .eq(UserDO::getEmail, dto.getEmail())) > 0) {
            throw exception(EMAIL_EXIST);
        }
        if (userMapper.selectCount(Wrappers.<UserDO>lambdaQuery()
                .eq(UserDO::getPhone, dto.getPhone())) > 0) {
            throw exception(PHONE_EXIST);
        }

            UserDO user = UserDO.builder()
                    .username(dto.getUsername())
                    .password(passwordEncoder.encode(dto.getPassword()))
                    .email(dto.getEmail())
                    .phone(dto.getPhone())
                    .remark(dto.getRemark())
                    .status(CommonStatusEnum.ENABLE.getStatus()) // 默认启用
                    .build();

            int rows = userMapper.insert(user);
            if (rows <= 0) {
                throw exception(INSERT_FAILURE);
            }
            return user;
        }


    @Override
    @Transactional
    public UserDO updateUser(UserUpdateDTO dto) {
        // 1) 存在性校验
        UserDO db = userMapper.selectById(dto.getId());
        if (db == null) {
            throw exception(USER_NOT_FOUND);
        }

        // 2) 业务唯一性校验（排除自己）
        if (dto.getUsername() != null &&
                userMapper.selectCount(Wrappers.<UserDO>lambdaQuery()
                        .eq(UserDO::getUsername, dto.getUsername())
                        .ne(UserDO::getId, dto.getId())) > 0) {
            throw exception(USERNAME_EXIST);
        }
        if (dto.getEmail() != null &&
                userMapper.selectCount(Wrappers.<UserDO>lambdaQuery()
                        .eq(UserDO::getEmail, dto.getEmail())
                        .ne(UserDO::getId, dto.getId())) > 0) {
            throw exception(EMAIL_EXIST);
        }
        if (dto.getPhone() != null &&
                userMapper.selectCount(Wrappers.<UserDO>lambdaQuery()
                        .eq(UserDO::getPhone, dto.getPhone())
                        .ne(UserDO::getId, dto.getId())) > 0) {
            throw exception(PHONE_EXIST);
        }

        // 3) 只更新非空字段
        LambdaUpdateWrapper<UserDO> uw = Wrappers.<UserDO>lambdaUpdate()
                .eq(UserDO::getId, dto.getId());
        if (dto.getUsername() != null) uw.set(UserDO::getUsername, dto.getUsername());
        if (dto.getEmail() != null) uw.set(UserDO::getEmail, dto.getEmail());
        if (dto.getPhone() != null) uw.set(UserDO::getPhone, dto.getPhone());
        if (dto.getRemark() != null) uw.set(UserDO::getRemark, dto.getRemark());

        if (userMapper.update(null, uw) <= 0) {
            throw exception(UPDATE_FAILURE);
        }

        // 4) 返回最新数据
        return userMapper.selectById(dto.getId());
    }

}
