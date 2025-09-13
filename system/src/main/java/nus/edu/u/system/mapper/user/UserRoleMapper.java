package nus.edu.u.system.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nus.edu.u.system.domain.dataobject.user.UserRoleDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * @author Lu Shuwen
 * @date 2025-09-10
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRoleDO> {


    // 当前有效角色（deleted = 0）
    List<Long> selectAliveRoleIdsByUser(@Param("userId") Long userId);

    // 批量逻辑删除：deleted=1
    int batchLogicalDelete(@Param("userId") Long userId,
                           @Param("roleIds") Collection<Long> roleIds);

    // 批量复活：deleted=0
    int batchRevive(@Param("userId") Long userId,
                    @Param("roleIds") Collection<Long> roleIds);

    // 幂等插入：存在则复活（依赖唯一键）
    int batchUpsertUserRoles(@Param("userId") Long userId,
                             @Param("roleIds") Collection<Long> roleIds);
}
