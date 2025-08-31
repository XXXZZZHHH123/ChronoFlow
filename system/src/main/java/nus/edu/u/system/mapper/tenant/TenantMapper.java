package nus.edu.u.system.mapper.tenant;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nus.edu.u.system.domain.dataobject.tenant.TenantDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author Lu Shuwen
 * @date 2025-08-31
 */
@Mapper
public interface TenantMapper extends BaseMapper<TenantDO> {
}
