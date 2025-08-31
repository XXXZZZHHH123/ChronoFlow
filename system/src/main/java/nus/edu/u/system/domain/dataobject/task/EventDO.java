package nus.edu.u.system.domain.dataobject.task;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import nus.edu.u.common.core.domain.base.TenantBaseDO;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.enums.event.EventStatusEnum;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Event data object from table event
 *
 * @author Lu Shuwen
 * @date 2025-08-28
 */
@TableName(value = "event")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventDO extends TenantBaseDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    /**
     * Related to {@link UserDO#getId()}
     */
    private Long userId;

    private String name;

    private String description;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /**
     * Event status
     *
     * Enum {@link EventStatusEnum}
     */
    private Integer status;

    private String remark;
}
