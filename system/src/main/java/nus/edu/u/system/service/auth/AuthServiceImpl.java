package nus.edu.u.system.service.auth;

import cn.hutool.core.bean.BeanUtil;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Validator;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.dto.TokenDTO;
import nus.edu.u.system.domain.dto.UserTokenDTO;
import nus.edu.u.system.domain.vo.auth.*;
import nus.edu.u.system.service.user.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import nus.edu.u.common.utils.validation.ValidationUtils;

import static nus.edu.u.common.exception.enums.GlobalErrorCodeConstants.EXPIRED_LOGIN_CREDENTIALS;
import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.*;

/**
 * Authentication service implementation
 *
 * @author Lu Shuwen
 * @date 2025-08-30
 */
@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Resource
    private UserService userService;

    @Resource
    private CaptchaService captchaService;

    @Resource
    private TokenService tokenService;

    @Resource
    private Validator validator;

    @Value("${chronoflow.captcha.enable:true}")
    @Setter
    private Boolean captchaEnable;

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
        // 1.Verify captcha
        ResponseModel response = validateCaptcha(reqVO);
        if (!response.isSuccess()) {
            throw exception(AUTH_LOGIN_CAPTCHA_CODE_ERROR, response.getRepMsg());
        }
        // 2.Verify username and password
        UserDO userDO = authenticate(reqVO.getUsername(), reqVO.getPassword());
        // 3.Create token
        return createTokenAfterLoginSuccess(userDO);
    }

    /**
     * Second verification of captcha
     */
    private ResponseModel validateCaptcha(CaptchaVerificationReqVO reqVO) {
        if (!captchaEnable) {
            return ResponseModel.success();
        }
        ValidationUtils.validate(validator, reqVO, CaptchaVerificationReqVO.CodeEnableGroup.class);
        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setCaptchaVerification(reqVO.getCaptchaVerification());
        return captchaService.verification(captchaVO);
    }

    private LoginRespVO createTokenAfterLoginSuccess(UserDO userDO) {
        // 1.Create UserTokenDTO which contains parameters required to create a token
        UserTokenDTO userTokenDTO = new UserTokenDTO();
        BeanUtil.copyProperties(userDO, userTokenDTO);
        // 2.Create two token and set parameters into response object
        TokenDTO accessToken = tokenService.createAccessToken(userTokenDTO);
        String refreshToken = tokenService.createRefreshToken(userTokenDTO);
        UserVO userVO = UserVO.builder().id(userDO.getId()).build();
        return LoginRespVO.builder()
                .accessToken(accessToken.getAccessToken())
                .accessTokenExpireTime(accessToken.getExpireTime())
                .refreshToken(refreshToken)
                .user(userVO).build();
    }

    @Override
    public void logout(String token) {
        tokenService.removeToken(token);
    }

    @Override
    public LoginRespVO refresh(String refreshToken) {
        // 1.Create access token and expire time
        TokenDTO tokenDTO = tokenService.refreshToken(refreshToken);
        // 2.If tokenDTO == null, throw an exception to re-login
        if (tokenDTO == null) {
            throw exception(EXPIRED_LOGIN_CREDENTIALS);
        }
        // 3.Build response object
        LoginRespVO loginRespVO = new LoginRespVO();
        BeanUtil.copyProperties(tokenDTO, loginRespVO);
        loginRespVO.setRefreshToken(refreshToken);
        return loginRespVO;
    }
}
