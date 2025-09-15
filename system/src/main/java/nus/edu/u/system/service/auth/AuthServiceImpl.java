package nus.edu.u.system.service.auth;

import static nus.edu.u.common.constant.Constants.DEFAULT_DELIMITER;
import static nus.edu.u.common.constant.Constants.SESSION_TENANT_ID;
import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.*;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.dto.RoleDTO;
import nus.edu.u.system.domain.dto.UserRoleDTO;
import nus.edu.u.system.domain.dto.UserTokenDTO;
import nus.edu.u.system.domain.vo.auth.*;
import nus.edu.u.system.service.user.UserService;
import org.springframework.stereotype.Service;

/**
 * Authentication service implementation
 *
 * @author Lu Shuwen
 * @date 2025-08-30
 */
@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Resource private UserService userService;

    @Resource private TokenService tokenService;

    @Override
    public UserDO authenticate(String username, String password) {
        // 1.Check username first
        UserDO userDO = userService.getUserByUsername(username);
        if (userDO == null) {
            throw exception(AUTH_LOGIN_BAD_CREDENTIALS);
        }
        // 2.Check password
        if (!userService.isPasswordMatch(password, userDO.getPassword())) {
            throw exception(AUTH_LOGIN_BAD_CREDENTIALS);
        }
        // 3.Check if user is disabled
        if (CommonStatusEnum.isDisable(userDO.getStatus())) {
            throw exception(AUTH_LOGIN_USER_DISABLED);
        }
        return userDO;
    }

    @Override
    public LoginRespVO login(LoginReqVO reqVO) {
        // 1.Verify username and password
        UserDO userDO = authenticate(reqVO.getUsername(), reqVO.getPassword());
        // 2.Update user login time
        userDO.setLoginTime(LocalDateTime.now());
        // 3.Create token
        return handleLogin(userDO, reqVO.isRemember(), reqVO.getRefreshToken());
    }

    private LoginRespVO handleLogin(UserDO userDO, boolean rememberMe, String refreshToken) {
        // 1.Create UserTokenDTO which contains parameters required to create a token
        UserTokenDTO userTokenDTO = new UserTokenDTO();
        BeanUtil.copyProperties(userDO, userTokenDTO);
        userTokenDTO.setRemember(rememberMe);
        // 2.Create two token and set parameters into response object
        StpUtil.login(userDO.getId());
        // 2.1 Set tenant id into context
        StpUtil.getSession().set(SESSION_TENANT_ID, userDO.getTenantId());
        // 3.Check if there already is a refresh token
        if (StrUtil.isEmpty(refreshToken)) {
            refreshToken = tokenService.createRefreshToken(userTokenDTO);
        }
        UserRoleDTO userRoleDTO = userService.selectUserWithRole(userDO.getId());
        UserVO userVO =
                UserVO.builder()
                        .id(userDO.getId())
                        .email(userDO.getEmail())
                        .name(userDO.getUsername())
                        .role(
                                userRoleDTO.getRoles().stream()
                                        .map(RoleDTO::getRoleKey)
                                        .collect(Collectors.joining(DEFAULT_DELIMITER)))
                        .build();
        return LoginRespVO.builder().refreshToken(refreshToken).user(userVO).build();
    }

    @Override
    public void logout(String token) {
        tokenService.removeToken(token);
        StpUtil.logout();
    }

    @Override
    public LoginRespVO refresh(String refreshToken) {
        // 1.Create access token and expire time
        Long userId = tokenService.getUserIdFromRefreshToken(refreshToken);
        if (ObjUtil.isNull(userId)) {
            throw exception(REFRESH_TOKEN_WRONG);
        }
        // 2.Login user
        StpUtil.login(userId);
        // 3.Build response object
        UserVO userVO = UserVO.builder().id(userId).build();
        return LoginRespVO.builder().refreshToken(refreshToken).user(userVO).build();
    }
}
