package nus.edu.u.system.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nus.edu.u.system.domain.dataobject.user.UserRoleDO;
import org.apache.ibatis.annotations.Delete;
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

    List<Long> selectAliveRoleIdsByUser(@Param("userId") Long userId);

    int batchLogicalDelete(@Param("userId") Long userId,
                           @Param("roleIds") Collection<Long> roleIds,
                           @Param("operator") String operator);

    int batchRevive(@Param("userId") Long userId,
                    @Param("roleIds") Collection<Long> roleIds,
                    @Param("operator") String operator);

    // Pass entities, ensuring each row has a unique id/tenantId/creator/updater
    int batchUpsertUserRoles(@Param("records") List<UserRoleDO> records);


}
