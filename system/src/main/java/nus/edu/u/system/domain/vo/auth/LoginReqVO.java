package nus.edu.u.system.domain.vo.auth;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

/** User login request VO */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginReqVO {

    @NotEmpty(message = "Username is required")
    @Size(min = 6, max = 100, message = "Username must be between 6 and 100 characters")
    private String username;

    @NotEmpty(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    private boolean remember = true;

    private String refreshToken;
}
