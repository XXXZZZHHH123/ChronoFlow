package nus.edu.u.system.domain.vo.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrganizerCreateUserReqVO {
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email format invalid")
    private String email;

    /** Directly pass the role ID list, at least one */
    @NotEmpty
    private List<@NotNull Long> roleIds;

    private String remark;
}
