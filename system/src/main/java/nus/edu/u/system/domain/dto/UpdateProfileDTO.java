package nus.edu.u.system.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UpdateProfileDTO {
    private long id;

    private String username;

    private String password;

    private String email;

    private String phone;

    private String remark;
}
