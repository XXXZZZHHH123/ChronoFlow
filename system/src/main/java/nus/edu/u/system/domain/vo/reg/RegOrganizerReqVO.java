package nus.edu.u.system.domain.vo.reg;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String name;

    @NotEmpty(message = "Username can't be empty")
    private String username;

    @NotEmpty(message = "Password can't be empty")
    private String userPassword;

    @NotEmpty(message = "User email can't be empty")
    private String userEmail;

    @NotEmpty(message = "User mobile can't be empty")
    private String mobile;

    @NotEmpty(message = "Organization name can't be empty")
    private String organizationName;

    private String organizationAddress;

    private String organizationCode;

}
