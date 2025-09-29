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
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.dto.RoleDTO;
import nus.edu.u.system.domain.dto.UserRoleDTO;
import nus.edu.u.system.domain.dto.UserTokenDTO;
import nus.edu.u.system.domain.vo.auth.LoginReqVO;
import nus.edu.u.system.domain.vo.auth.LoginRespVO;
import nus.edu.u.system.domain.vo.auth.UserVO;
import nus.edu.u.system.domain.vo.role.RoleRespVO;
import nus.edu.u.system.service.role.RoleService;
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

    @Resource private RoleService roleService;

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
        return getInfo(refreshToken);
    }

    @Override
    public void logout(String token) {
        tokenService.removeToken(token);
        StpUtil.logout();
    }

    @Override
    public LoginRespVO refresh(String refreshToken) {
        // Check if user login or not
        if (StpUtil.isLogin()) {
            return getInfo(refreshToken);
        }
        // Login expired
        // Create access token and expire time
        Long userId = tokenService.getUserIdFromRefreshToken(refreshToken);
        if (ObjUtil.isNull(userId)) {
            throw exception(REFRESH_TOKEN_WRONG);
        }
        // Login user
        StpUtil.login(userId);
        // Build response object
        return getInfo(refreshToken);
    }

    private LoginRespVO getInfo(String refreshToken) {
        UserRoleDTO userRoleDTO =
                userService.selectUserWithRole(Long.parseLong(StpUtil.getLoginId().toString()));
        if (userRoleDTO == null) {
            throw exception(ACCOUNT_ERROR);
        }
        UserVO userVO =
                UserVO.builder()
                        .id(userRoleDTO.getUserId())
                        .email(userRoleDTO.getEmail())
                        .name(userRoleDTO.getUsername())
                        .role(
                                userRoleDTO.getRoles().stream()
                                        .map(RoleDTO::getRoleKey)
                                        .collect(Collectors.joining(DEFAULT_DELIMITER)))
                        .build();
        List<RoleRespVO> roleRespVOList = userRoleDTO.getRoles().stream()
                .map(role -> roleService.getRole(role.getId()))
                .filter(ObjUtil::isNotNull)
                .toList();
        return LoginRespVO.builder()
                .refreshToken(refreshToken)
                .user(userVO)
                .roles(roleRespVOList)
                .build();
    }
}
