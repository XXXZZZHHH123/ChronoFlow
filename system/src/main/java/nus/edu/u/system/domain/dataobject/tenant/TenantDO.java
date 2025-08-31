package nus.edu.u.system.domain.dataobject.tenant;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.common.core.domain.base.BaseDO;
import nus.edu.u.system.domain.dataobject.user.UserDO;

import java.io.Serializable;

/**
 * Tenant data object for table sys_tenant
 *
 * @author Lu Shuwen
 * @date 2025-08-28
 */
@TableName(value = "sys_tenant")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenantDO extends BaseDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    private String name;

    /**
     * Related to {@link UserDO#getId()}
     */
    private Long contactUserId;

    private String contactName;

    private String contactMobile;

    /**
     * Tenant status
     *
     * Enum {@link CommonStatusEnum}
     */
    private Integer status;

    private String tenantCode;

}
