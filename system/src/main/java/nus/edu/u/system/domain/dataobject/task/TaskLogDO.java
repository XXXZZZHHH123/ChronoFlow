package nus.edu.u.system.domain.dataobject.task;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import nus.edu.u.common.core.domain.base.TenantBaseDO;
import nus.edu.u.system.enums.task.TaskActionEnum;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Task log data object for table task_log
 *
 * @author Lu Shuwen
 * @date 2025-08-28
 */
@TableName(value = "task_log")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskLogDO extends TenantBaseDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    /**
     * Related to {@link TaskDO#getId()}
     */
    private Long taskId;

    /**
     * Related to {@link TaskActionEnum}
     */
    private Integer action;

    private BigDecimal moneyCost;

    private BigDecimal laborCost;

    private String remark;

}
