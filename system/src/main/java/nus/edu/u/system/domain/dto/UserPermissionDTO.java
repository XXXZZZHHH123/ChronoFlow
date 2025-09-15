package nus.edu.u.system.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User id with his all permission
 *
 * @author Lu Shuwen
 * @date 2025-09-14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserPermissionDTO {

    private Long userId;

    private Long permissionId;

    private String permissionName;

    private String permissionKey;

    private Integer permissionType;

    private Long parentId;

    private Integer status;
}
