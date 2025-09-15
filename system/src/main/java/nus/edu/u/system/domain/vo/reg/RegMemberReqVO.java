package nus.edu.u.system.domain.vo.reg;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Register request VO
 *
 * @author Lu Shuwen
 * @date 2025-09-10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegMemberReqVO {

    private Long userId;

    @NotEmpty(message = "Please set your username")
    @Size(min = 6, max = 100, message = "Username length should between 6 and 100")
    private String username;

    @NotEmpty(message = "Please set your password")
    @Size(min = 8, max = 100, message = "Password length should between 8 and 100")
    private String password;

    @NotEmpty(message = "Please key in your phone number")
    private String phone;
}
