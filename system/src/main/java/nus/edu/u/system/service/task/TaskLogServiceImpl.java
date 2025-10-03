package nus.edu.u.system.service.task;

import jakarta.annotation.Resource;
import nus.edu.u.system.domain.dataobject.task.TaskLogDO;
import nus.edu.u.system.mapper.task.TaskLogMapper;
import org.springframework.stereotype.Service;

/**
 * @author Lu Shuwen
 * @date 2025-10-02
 */
@Service
public class TaskLogServiceImpl implements TaskLogService {

    @Resource private TaskLogMapper taskLogMapper;

    @Override
    public boolean insertTaskLog(Long taskId, Long targetUserId, Integer action) {
        TaskLogDO taskLogDO =
                TaskLogDO.builder()
                        .taskId(taskId)
                        .targetUserId(targetUserId)
                        .action(action)
                        .build();
        return taskLogMapper.insert(taskLogDO) > 0;
    }
}
