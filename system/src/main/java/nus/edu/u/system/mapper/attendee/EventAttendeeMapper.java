package nus.edu.u.system.mapper.attendee;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import nus.edu.u.system.domain.dataobject.attendee.EventAttendeeDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * Event Attendee Mapper
 *
 * @author Fan Yazhuoting
 * @date 2025-10-07
 */
@Mapper
public interface EventAttendeeMapper extends BaseMapper<EventAttendeeDO> {

    /** 通过 token 查询 */
    default EventAttendeeDO selectByToken(String token) {
        return this.selectOne(
                Wrappers.<EventAttendeeDO>lambdaQuery()
                        .eq(EventAttendeeDO::getCheckInToken, token)
                        .eq(EventAttendeeDO::getDeleted, false)
                        .last("LIMIT 1"));
    }

    /** 通过 eventId 和 email 查询 */
    default EventAttendeeDO selectByEventAndEmail(Long eventId, String email) {
        return this.selectOne(
                Wrappers.<EventAttendeeDO>lambdaQuery()
                        .eq(EventAttendeeDO::getEventId, eventId)
                        .eq(EventAttendeeDO::getAttendeeEmail, email)
                        .eq(EventAttendeeDO::getDeleted, false)
                        .last("LIMIT 1"));
    }

    /** 通过 eventId 查询所有 attendees */
    default List<EventAttendeeDO> selectByEventId(Long eventId) {
        return this.selectList(
                Wrappers.<EventAttendeeDO>lambdaQuery().eq(EventAttendeeDO::getEventId, eventId));
    }
}
