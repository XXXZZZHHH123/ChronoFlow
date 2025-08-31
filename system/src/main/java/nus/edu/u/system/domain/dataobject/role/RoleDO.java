package nus.edu.u.system.domain.dataobject.role;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.common.core.domain.base.TenantBaseDO;
import nus.edu.u.system.enums.role.RoleTypeEnum;

import java.io.Serializable;

/**
 * Role data object for table sys_role
 *
 * @author Lu Shuwen
 * @date 2025-08-28
 */
@TableName(value = "sys_role")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoleDO extends TenantBaseDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    private String name;

    private Integer level;

    /**
     * 1 - system role; 2 - custom role
     *
     * Enum {@link RoleTypeEnum}
     */
    private Integer type;

    /**
     * Role status
     *
     * Enum {@link CommonStatusEnum}
     */
    private Integer status;

    private String remark;
}
