package nus.edu.u.system.domain.dataobject.permission;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import lombok.*;
import nus.edu.u.common.core.domain.base.TenantBaseDO;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.system.enums.permission.PermissionTypeEnum;

/**
 * @author Lu Shuwen
 * @date 2025-09-10
 */
@TableName(value = "sys_permission")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PermissionDO extends TenantBaseDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId private Long id;

    private String name;

    /** Key to judge the permission */
    private String permissionKey;

    /**
     * 1-menu 2-button 3-API
     *
     * <p>Enum {@link PermissionTypeEnum}
     */
    private Integer type;

    /** To store parent permission id */
    private Long parentId;

    /**
     * Permission status
     *
     * <p>Enum {@link CommonStatusEnum}
     */
    private Integer status;
}
