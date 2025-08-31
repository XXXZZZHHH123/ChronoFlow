package nus.edu.u.system.domain.dataobject.dept;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import nus.edu.u.common.core.domain.base.TenantBaseDO;
import nus.edu.u.system.domain.dataobject.user.UserDO;

import java.io.Serializable;

/**
 * User position data object for table sys_user_post
 *
 * @author Lu Shuwen
 * @date 2025-08-28
 */
@TableName(value = "sys_user_post")
@Data
@EqualsAndHashCode(callSuper = true)
public class UserPostDO extends TenantBaseDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    /**
     * Related to {@link UserDO#getId()}
     */
    private Long userId;

    /**
     * Related to {@link PostDO#getId()}
     */
    private Long postId;
}
