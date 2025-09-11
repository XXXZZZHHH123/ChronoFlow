package nus.edu.u.system.domain.dataobject.user;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import nus.edu.u.common.core.domain.base.TenantBaseDO;
import nus.edu.u.system.domain.dataobject.role.RoleDO;

import java.io.Serializable;

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
}
