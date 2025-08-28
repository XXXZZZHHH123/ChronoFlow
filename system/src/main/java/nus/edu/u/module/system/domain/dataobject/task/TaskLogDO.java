package nus.edu.u.module.system.domain.dataobject.task;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import nus.edu.u.common.core.domain.base.TenantBaseDO;
import nus.edu.u.module.system.enums.task.TaskActionEnum;

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
public class TaskLogDO extends TenantBaseDO {

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
