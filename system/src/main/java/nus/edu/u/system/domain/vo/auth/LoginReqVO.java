package nus.edu.u.system.domain.vo.auth;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

/**
 * User login request VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginReqVO extends CaptchaVerificationReqVO {

    @NotEmpty(message = "Username can't be empty")
    private String username;

    @NotEmpty(message = "Passage can't be empty")
    private String password;

}