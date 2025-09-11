package nus.edu.u.system.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCreateDTO {
    @NotBlank
    @Size(min = 4, max = 32)
    private String username;

    @NotBlank
    @Size(min = 8, max = 128)
    private String password;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String phone;

    private String remark;
}
