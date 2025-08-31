package nus.edu.u.system.domain.vo.auth;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class CaptchaVerificationReqVO {

    // ========== 图片验证码相关 ==========
    @NotEmpty(message = "Captcha can't be empty", groups = CodeEnableGroup.class)
    private String captchaVerification;

    /**
     * 开启验证码的 Group
     */
    public interface CodeEnableGroup {
    }
}
