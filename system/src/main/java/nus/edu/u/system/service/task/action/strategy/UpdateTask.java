package nus.edu.u.system.service.task.action.strategy;

import cn.hutool.core.util.ObjectUtil;
import java.time.LocalDateTime;
import nus.edu.u.system.domain.dataobject.task.TaskDO;
import nus.edu.u.system.domain.dto.TaskActionDTO;
import nus.edu.u.system.enums.task.TaskActionEnum;
import nus.edu.u.system.enums.task.TaskStatusEnum;
import nus.edu.u.system.service.task.action.AbstractTaskStrategy;
import org.springframework.stereotype.Component;

import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.TASK_UPDATE_FAILED;

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
    public void execute(TaskDO task, TaskActionDTO actionDTO, Object... params) {
        validateTimeRange(
                task,
                task.getStartTime(),
                task.getEndTime(),
                actionDTO.getEventStartTime(),
                actionDTO.getEventEndTime());
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
        task.setUserId(actionDTO.getTargetUserId());
        boolean isSuccess = taskMapper.updateById(task) > 0;
        if (isSuccess) {
            throw exception(TASK_UPDATE_FAILED);
        }
        taskLogService.insertTaskLog(task.getId(), null, getType().getCode());
    }
}
