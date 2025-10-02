package nus.edu.u.system.domain.dataobject.attendee;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import nus.edu.u.common.core.domain.base.TenantBaseDO;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author Lu Shuwen
 * @date 2025-10-02
 */
@TableName(value = "attendee", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AttendeeDO extends TenantBaseDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    private String name;

    private String email;

}
