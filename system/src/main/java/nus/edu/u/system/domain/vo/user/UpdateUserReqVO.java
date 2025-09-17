package nus.edu.u.system.domain.vo.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class UpdateUserReqVO {

    @Email(message = "Email format invalid")
    private String email;

    /** Directly pass the role ID list, at least one */
    @NotEmpty(message = "RoleIds cannot be empty")
    private List<@NotNull Long> roleIds;

    private String remark;
}
