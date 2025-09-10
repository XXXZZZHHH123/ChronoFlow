package nus.edu.u.system.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nus.edu.u.system.domain.dataobject.user.UserRoleDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author Lu Shuwen
 * @date 2025-09-10
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRoleDO> {
}
