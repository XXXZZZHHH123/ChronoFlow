package nus.edu.u.system.domain.vo.auth;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
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
    @Size(min = 6, max = 100, message = "Username length should between 6 and 100")
    private String username;

    @NotEmpty(message = "Passage can't be empty")
    @Size(min = 8, max = 100, message = "Password length should between 8 and 100")
    private String password;

    private boolean remember = true;

    private String refreshToken;

}