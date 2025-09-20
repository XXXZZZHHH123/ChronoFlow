package nus.edu.u.system.mapper.task;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nus.edu.u.system.domain.dataobject.task.EventDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * @author Lu Shuwen
 * @date 2025-08-31
 */
@Mapper
public interface EventMapper extends BaseMapper<EventDO> {
    @Select("SELECT * FROM event WHERE id = #{id}")
    EventDO selectRawById(@Param("id") Long id);

    @Update(
            """
        UPDATE event
        SET deleted = 0, update_time = NOW()
        WHERE id = #{id} AND deleted = 1
    """)
    int restoreById(@Param("id") Long id);
}
