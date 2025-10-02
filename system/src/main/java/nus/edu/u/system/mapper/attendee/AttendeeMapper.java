package nus.edu.u.system.mapper.attendee;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nus.edu.u.system.domain.dataobject.attendee.AttendeeDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author Lu Shuwen
 * @date 2025-10-02
 */
@Mapper
public interface AttendeeMapper extends BaseMapper<AttendeeDO> {
}
