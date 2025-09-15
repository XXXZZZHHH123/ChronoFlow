package nus.edu.u.system.domain.dataobject.role;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.*;
import nus.edu.u.common.core.domain.base.TenantBaseDO;
import nus.edu.u.system.domain.dataobject.permission.PermissionDO;

/**
 * @author Lu Shuwen
 * @date 2025-09-10
 */
@TableName(value = "sys_role_permission")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RolePermissionDO extends TenantBaseDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId private Long id;

    /** Related to {@link RoleDO#getId()} */
    private Long roleId;

    /** Related to {@link PermissionDO#getId()} */
    private Long permissionId;
}
