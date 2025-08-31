package nus.edu.u.system.domain.vo.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author Lu Shuwen
 * @date 2025-08-31
 */
@Data
public class RefreshTokenVO {

    @NotBlank(message = "Refresh token can't be empty")
    private String refreshToken;
}
