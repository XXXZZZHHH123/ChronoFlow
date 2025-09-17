package nus.edu.u.system.domain.vo.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileReqVO {
    @Size(min = 4, max = 32, message = "Username length must be 4~32")
    private String username;

    @Size(min = 8, max = 128, message = "Password length must be 8~128")
    private String password;

    @Email(message = "Email format invalid")
    private String email;

    private String phone;

    private String remark;
}
