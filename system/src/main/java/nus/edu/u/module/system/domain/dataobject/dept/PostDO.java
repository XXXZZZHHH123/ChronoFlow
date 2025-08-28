package nus.edu.u.module.system.domain.dataobject.dept;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.module.system.domain.dataobject.base.TenantBaseDO;

/**
 * Position data object for table sys_post
 *
 * @author Lu Shuwen
 * @date 2025-08-28
 */
@TableName(value = "sys_post")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String name;

    private Integer sort;

    /**
     * Post status
     *
     * Enum {@link CommonStatusEnum}
     */
    private Integer status;

    private String remark;
}
