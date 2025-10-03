package nus.edu.u.system.service.task.action.strategy;

import cn.hutool.core.util.ObjectUtil;
import java.time.LocalDateTime;
import nus.edu.u.system.domain.dataobject.task.TaskDO;
import nus.edu.u.system.enums.task.TaskActionEnum;
import nus.edu.u.system.enums.task.TaskStatusEnum;
import nus.edu.u.system.service.task.action.AbstractTaskStrategy;
import org.springframework.stereotype.Component;

/**
 * @author Lu Shuwen
 * @date 2025-10-02
 */
@Component
public class UpdateTask extends AbstractTaskStrategy {

    @Override
    public TaskActionEnum getType() {
        return TaskActionEnum.UPDATE;
    }

    @Override
    public boolean execute(TaskDO task, Long targetUserId, Object... params) {
        validateTimeRange(
                task.getStartTime(),
                task.getEndTime(),
                (LocalDateTime) params[0],
                (LocalDateTime) params[1]);
        if (ObjectUtil.equals(task.getStatus(), TaskStatusEnum.BLOCKED.getStatus())
                || ObjectUtil.equals(task.getStatus(), TaskStatusEnum.COMPLETED.getStatus())
                || ObjectUtil.equals(task.getStatus(), TaskStatusEnum.DELAYED.getStatus())
                || ObjectUtil.equals(
                        task.getStatus(), TaskStatusEnum.PENDING_APPROVAL.getStatus())) {
            task.setStatus(TaskStatusEnum.PROGRESS.getStatus());
        }
        if (ObjectUtil.equals(task.getStatus(), TaskStatusEnum.REJECTED.getStatus())) {
            task.setStatus(TaskStatusEnum.PENDING.getStatus());
        }
        task.setUserId(targetUserId);
        boolean isSuccess = taskMapper.updateById(task) > 0;
        return isSuccess && taskLogService.insertTaskLog(task.getId(), null, getType().getCode());
    }
}
