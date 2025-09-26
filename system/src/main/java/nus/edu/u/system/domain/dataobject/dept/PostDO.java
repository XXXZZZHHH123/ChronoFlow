package nus.edu.u.system.domain.dataobject.dept;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import lombok.*;
import nus.edu.u.common.core.domain.base.TenantBaseDO;
import nus.edu.u.common.enums.CommonStatusEnum;

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
public class PostDO extends TenantBaseDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId private Long id;

    private String name;

    private Integer sort;

    /**
     * Post status
     *
     * <p>Enum {@link CommonStatusEnum}
     */
    private Integer status;

    private String remark;
}
