package nus.edu.u.system.domain.dataobject.user;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import nus.edu.u.common.core.domain.base.TenantBaseDO;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName(value = "sys_user_group")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserGroupDO extends TenantBaseDO implements Serializable {

    @TableId
    private Long id;

    private Long userId;

    private Long deptId;

    private Long eventId;

    private LocalDateTime joinTime;

    private Integer roleType;
}