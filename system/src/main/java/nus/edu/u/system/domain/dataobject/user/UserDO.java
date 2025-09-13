package nus.edu.u.system.domain.dataobject.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;
import nus.edu.u.system.domain.dataobject.role.RoleDO;
import nus.edu.u.common.core.domain.base.TenantBaseDO;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.system.domain.dataobject.dept.DeptDO;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * User data object for table sys_user
 *
 * @author Lu Shuwen
 * @date 2025-08-27
 */
@TableName(value = "sys_user", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDO extends TenantBaseDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    private String username;

    private String password;

    private String remark;

    private String email;

    private String phone;

    /**
     * User status
     *
     * Enum {@link CommonStatusEnum}
     */
    private Integer status;

    private LocalDateTime loginTime;

    /**
     * Related to {@link DeptDO#getId()}
     */
    private Long DeptId;

    @TableField(typeHandler = JacksonTypeHandler.class, value = "post_list")
    private List<Integer> postList;

}
