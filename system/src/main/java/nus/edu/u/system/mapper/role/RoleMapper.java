package nus.edu.u.system.mapper.role;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import nus.edu.u.system.domain.dataobject.role.RoleDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author Lu Shuwen
 * @date 2025-08-31
 */
@Mapper
public interface RoleMapper extends BaseMapper<RoleDO> {
    /**
     * Batch counts whether role IDs exist
     *
     * @param ids A collection of role IDs
     * @return The actual number of existing roles
     */
    int countByIds(@Param("ids") List<Long> ids);
}
