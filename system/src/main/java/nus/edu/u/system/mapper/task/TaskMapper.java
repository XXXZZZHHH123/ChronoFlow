package nus.edu.u.system.mapper.task;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import nus.edu.u.system.domain.dataobject.task.TaskDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author Lu Shuwen
 * @date 2025-08-31
 */
@Mapper
public interface TaskMapper extends BaseMapper<TaskDO> {}
