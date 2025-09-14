package nus.edu.u.system.service.user;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.dto.UserRoleDTO;
import nus.edu.u.system.mapper.user.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
    public UserDO selectUserById(Long userId) {
        return userMapper.selectById(userId);
    }

}
