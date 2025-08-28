package nus.edu.u.module.system.domain.dataobject.file;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import nus.edu.u.module.system.domain.dataobject.task.EventDO;
import nus.edu.u.common.core.domain.base.TenantBaseDO;
import nus.edu.u.module.system.domain.dataobject.task.TaskLogDO;

/**
 * File data object for table file
 *
 * @author Lu Shuwen
 * @date 2025-08-28
 */
@TableName(value = "file")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileDO extends TenantBaseDO {

    @TableId
    private Long id;

    /**
     * Related to {@link TaskLogDO#getId()}
     */
    private Long taskLogId;

    /**
     * Related to {@link EventDO#getId()}
     */
    private Long eventId;

    private String name;

    private String url;

    private String type;

    private Long size;

}
