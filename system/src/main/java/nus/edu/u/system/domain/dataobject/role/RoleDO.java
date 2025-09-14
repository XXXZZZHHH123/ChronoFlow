package nus.edu.u.system.domain.dataobject.role;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.*;
import nus.edu.u.common.core.domain.base.TenantBaseDO;
import nus.edu.u.common.enums.CommonStatusEnum;

/**
 * Role data object for table sys_role
 *
 * @author Lu Shuwen
 * @date 2025-08-28
 */
@TableName(value = "sys_role")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoleDO extends TenantBaseDO implements Serializable {

  private static final long serialVersionUID = 1L;

  @TableId private Long id;

  private String name;

  private String roleKey;

  /**
   * Role status
   *
   * <p>Enum {@link CommonStatusEnum}
   */
  private Integer status;

  private String remark;
}
