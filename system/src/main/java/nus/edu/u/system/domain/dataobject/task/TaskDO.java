package nus.edu.u.system.domain.dataobject.task;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import nus.edu.u.common.enums.task.TaskStatusEnum;
import nus.edu.u.system.domain.dataobject.base.TenantBaseDO;
import nus.edu.u.system.domain.dataobject.user.UserDO;

import java.time.LocalDateTime;

/**
 * Task data object for table task
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
public class TaskDO extends TenantBaseDO {

    @TableId
    private Long id;

    /**
     * Related to {@link UserDO#getId()}
     */
    private Long userId;

    /**
     * Related to {@link EventDO#getId()}
     */
    private Long eventId;

    private String name;

    private String description;

    /**
     * Related to {@link TaskStatusEnum}
     */
    private String status;

    private String remark;

    private LocalDateTime startTime;

    private LocalDateTime endDTime;
}
