package nus.edu.u.system.domain.vo.auth;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

/**
 * User login request VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginReqVO {

    @NotEmpty(message = "Username can't be empty")
    private String username;

    @NotEmpty(message = "Passage can't be empty")
    private String password;

    private boolean remember = true;

    private String refreshToken;

}