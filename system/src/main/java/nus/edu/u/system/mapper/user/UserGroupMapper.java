package nus.edu.u.system.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nus.edu.u.system.domain.dataobject.user.UserGroupDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * User-Group associated Mapper
 *
 * @author Fan yazhuoting
 * @date 2025-10-04
 */
@Mapper
public interface UserGroupMapper extends BaseMapper<UserGroupDO> {}
