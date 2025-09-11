package nus.edu.u.system.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Use for store union table query result
 *
 * @author Lu Shuwen
 * @date 2025-09-10
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRoleDTO {

    private Long userId;

    private String username;

    private String email;

    private Long tenantId;

    private List<RoleDTO> roles;

}
