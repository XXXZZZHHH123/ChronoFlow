package nus.edu.u.system.service.user;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.common.utils.validation.ValidationUtils;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.dto.UserCreateDTO;
import nus.edu.u.system.mapper.user.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
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
    public Long createUser(UserCreateDTO dto) {
        ValidationUtils.validate(dto);
        if (!ValidationUtils.isMobile(dto.getPhone())){
            throw exception(WRONG_MOBILE);
        }
        // Username/email uniqueness check
        Long usernameCount = userMapper.selectCount(Wrappers.<UserDO>lambdaQuery()
                .eq(UserDO::getUsername, dto.getUsername()));
        Long emailCount = userMapper.selectCount(Wrappers.<UserDO>lambdaQuery()
                .eq(UserDO::getEmail, dto.getEmail()));
        if(usernameCount > 0) {
            throw exception(USERNAME_EXIST);
        }
        if(emailCount > 0) {
            throw exception(EMAIL_EXIST);
        }

        UserDO user = UserDO.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .remark(dto.getRemark())
                .status(CommonStatusEnum.ENABLE.getStatus()) // 默认启用
                .build();

        userMapper.insert(user);
        return user.getId();
    }

}
