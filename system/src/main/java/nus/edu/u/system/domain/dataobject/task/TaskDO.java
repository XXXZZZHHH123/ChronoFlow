package nus.edu.u.system.domain.dataobject.task;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.*;
import nus.edu.u.common.core.domain.base.TenantBaseDO;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.enums.task.TaskStatusEnum;

/**
 * task data object for table task
 *
 * @author Lu Shuwen
 * @date 2025-08-28
 */
@TableName(value = "task")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskDO extends TenantBaseDO implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    @TableId private Long id;

    /** Related to {@link UserDO#getId()} */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long userId;

    /** Related to {@link EventDO#getId()} */
    private Long eventId;

    private String name;

    private String description;

    /** Related to {@link TaskStatusEnum} */
    private Integer status;

    private String remark;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
