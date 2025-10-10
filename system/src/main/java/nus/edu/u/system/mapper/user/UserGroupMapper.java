package nus.edu.u.system.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nus.edu.u.system.domain.dataobject.user.UserGroupDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * User-Group associated Mapper
 *
 * @author Fan yazhuoting
 * @date 2025-10-04
 */
@Mapper
public interface UserGroupMapper extends BaseMapper<UserGroupDO> {

    /**
     * Restore logically deleted user-group relations for a given event.
     */
    @Update(
            "UPDATE sys_user_group "
                    + "SET deleted = 0, update_time = NOW() "
                    + "WHERE event_id = #{eventId} AND deleted = 1")
    int restoreByEventId(@Param("eventId") Long eventId);
}
