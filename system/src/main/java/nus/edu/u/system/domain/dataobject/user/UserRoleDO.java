package nus.edu.u.system.domain.dataobject.user;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import nus.edu.u.common.core.domain.base.TenantBaseDO;
import nus.edu.u.system.domain.dataobject.role.RoleDO;

/**
 * @author Lu Shuwen
 * @date 2025-09-10
 */
@TableName(value = "sys_user_role")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRoleDO extends TenantBaseDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    /**
     * Related to {@link UserDO#getId()}
     */
    private Long userId;

    /**
     * Related to {@link RoleDO#getId()}
     */
    private Long roleId;

    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private String creator;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updater;

    @TableLogic
    private Boolean deleted;
}
