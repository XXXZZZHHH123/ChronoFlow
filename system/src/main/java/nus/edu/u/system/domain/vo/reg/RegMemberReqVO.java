package nus.edu.u.system.domain.vo.reg;

import jakarta.validation.constraints.NotEmpty;
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
    private String username;

    @NotEmpty(message = "Please set your password")
    private String password;

    @NotEmpty(message = "Please key in your phone number")
    private String phone;

}
