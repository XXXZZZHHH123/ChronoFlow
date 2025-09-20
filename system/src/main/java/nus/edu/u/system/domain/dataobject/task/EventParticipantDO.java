package nus.edu.u.system.domain.dataobject.task;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.*;
import nus.edu.u.common.core.domain.base.TenantBaseDO;

@TableName("event_participant")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventParticipantDO extends TenantBaseDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId private Long id;

    private Long eventId;

    private Long userId;
}
