package nus.edu.u.system.service.task.action.strategy;

import java.time.LocalDateTime;
import nus.edu.u.system.domain.dataobject.task.TaskDO;
import nus.edu.u.system.enums.task.TaskActionEnum;
import nus.edu.u.system.enums.task.TaskStatusEnum;
import nus.edu.u.system.service.task.action.AbstractTaskStrategy;
import org.springframework.stereotype.Component;

/**
 * @author Lu Shuwen
 * @date 2025-10-03
 */
@Component
public class SubmitTask extends AbstractTaskStrategy {

    @Override
    public TaskActionEnum getType() {
        return TaskActionEnum.SUBMIT;
    }

    @Override
    public boolean execute(TaskDO task, Long targetUserId, Object... params) {
        validateTimeRange(
                task.getStartTime(),
                task.getEndTime(),
                (LocalDateTime) params[0],
                (LocalDateTime) params[1]);
        task.setStatus(TaskStatusEnum.PENDING_APPROVAL.getStatus());
        boolean isSuccess = taskMapper.updateById(task) > 0;
        return isSuccess
                && taskLogService.insertTaskLog(task.getId(), targetUserId, getType().getCode());
    }
}
