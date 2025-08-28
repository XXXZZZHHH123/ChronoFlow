package nus.edu.u.module.system.domain.dataobject.dept;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.module.system.domain.dataobject.base.TenantBaseDO;
import nus.edu.u.module.system.domain.dataobject.user.UserDO;

/**
 * Department data object for table sys_dept
 *
 * @author Lu Shuwen
 * @date 2025-08-28
 */
@TableName(value = "sys_dept")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeptDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String name;

    private Integer sort;

    /**
     * Related to {@link UserDO#getId()}
     */
    private Long leadUserId;

    private String phone;

    private String email;

    private String remark;

    /**
     * Department status
     *
     * Enum {@link CommonStatusEnum}
     */
    private Integer status;

}
