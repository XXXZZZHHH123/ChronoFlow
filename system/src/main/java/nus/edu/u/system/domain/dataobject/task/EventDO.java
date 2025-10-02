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
import nus.edu.u.system.enums.event.EventStatusEnum;

/**
 * Event data object from table event
 *
 * @author Lu Shuwen
 * @date 2025-08-28
 */
@TableName(value = "event")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class EventDO extends TenantBaseDO implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    @TableId private Long id;

    /** Related to {@link UserDO#getId()} */
    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private Long userId;

    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private String name;

    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private String description;

    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private LocalDateTime startTime;

    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private String location;

    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private LocalDateTime endTime;

    /**
     * Event status
     *
     * <p>Enum {@link EventStatusEnum}
     */
    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private Integer status;

    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private String remark;
}
