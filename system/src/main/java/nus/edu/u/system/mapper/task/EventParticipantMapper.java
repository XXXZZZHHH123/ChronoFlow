package nus.edu.u.system.mapper.task;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nus.edu.u.system.domain.dataobject.task.EventParticipantDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface EventParticipantMapper extends BaseMapper<EventParticipantDO> {

    /**
     * Restore deleted participants for an event
     * Note: This will restore all participants but their group assignments (deptId)
     * will remain null and need to be reassigned
     */
    @Update(
            "UPDATE event_participant "
                    + "SET deleted = 0, update_time = NOW() "
                    + "WHERE event_id = #{eventId} AND deleted = 1")
    int restoreByEventId(@Param("eventId") Long eventId);
}