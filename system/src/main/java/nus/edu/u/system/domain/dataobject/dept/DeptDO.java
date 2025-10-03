package nus.edu.u.system.domain.dataobject.dept;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serial;
import java.io.Serializable;
import lombok.*;
import nus.edu.u.common.core.domain.base.TenantBaseDO;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.system.domain.dataobject.task.EventDO;
import nus.edu.u.system.domain.dataobject.user.UserDO;

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
public class DeptDO extends TenantBaseDO implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    @TableId private Long id;

    private String name;

    private Integer sort;

    /** Related to {@link UserDO#getId()} */
    private Long leadUserId;

    private String remark;

    /**
     * Department status
     *
     * <p>Enum {@link CommonStatusEnum}
     */
    private Integer status;

    /**
     * Associated event ID
     *
     * <p>Related to {@link EventDO#getId()}
     */
    private Long eventId;
}
