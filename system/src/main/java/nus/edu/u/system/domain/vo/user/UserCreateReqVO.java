package nus.edu.u.system.domain.vo.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreateReqVO {
    @NotBlank(message = "Username cannot be blank")
    @Size(min = 4, max = 32, message = "Username length must be 4~32")
    private String username;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, max = 128, message = "Password length must be 8~128")
    private String password;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email format invalid")
    private String email;

    @NotBlank(message = "Phone cannot be blank")
    private String phone;

    private String remark;
}
