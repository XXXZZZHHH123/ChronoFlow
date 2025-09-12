package nus.edu.u.system.domain.vo.reg;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nus.edu.u.common.annotation.Mobile;

/**
 * @author Lu Shuwen
 * @date 2025-09-10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegOrganizerReqVO {

    @NotEmpty(message = "Organizer name can't be empty")
    @Size(max = 100, message = "Organizer name length should within 100")
    private String name;

    @NotEmpty(message = "Username can't be empty")
    @Size(min = 6, max = 100, message = "Username length should between 6 and 100")
    private String username;

    @NotEmpty(message = "Password can't be empty")
    @Size(min = 8, max = 100, message = "Password length should between 8 and 100")
    private String userPassword;

    @NotEmpty(message = "User email can't be empty")
    @Email(message = "Please input right format email")
    private String userEmail;

    @NotEmpty(message = "User mobile can't be empty")
    private String mobile;

    @NotEmpty(message = "Organization name can't be empty")
    @Size(max = 100, message = "Organization name length should within 100")
    private String organizationName;

    @Size(max = 500, message = "Organization address length should within 500")
    private String organizationAddress;

    @Size(min = 6, max = 20, message = "Organization code length should between 6 and 20")
    private String organizationCode;

}
