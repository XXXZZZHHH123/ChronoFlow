package nus.edu.u.system.service.task.action.strategy;

import java.time.LocalDateTime;
import nus.edu.u.system.domain.dataobject.task.TaskDO;
import nus.edu.u.system.domain.dto.TaskActionDTO;
import nus.edu.u.system.enums.task.TaskActionEnum;
import nus.edu.u.system.enums.task.TaskStatusEnum;
import nus.edu.u.system.service.task.action.AbstractTaskStrategy;
import org.springframework.stereotype.Component;

import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.SUBMIT_TASK_FAILED;

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
    public void execute(TaskDO task, TaskActionDTO actionDTO, Object... params) {
        validateTimeRange(
                task,
                task.getStartTime(),
                task.getEndTime(),
                actionDTO.getEventStartTime(),
                actionDTO.getEventEndTime());
        task.setStatus(TaskStatusEnum.PENDING_APPROVAL.getStatus());
        boolean isSuccess = taskMapper.updateById(task) > 0;
        if (!isSuccess) {
            throw exception(SUBMIT_TASK_FAILED);
        }
        taskLogService.insertTaskLog(task.getId(), null, getType().getCode());
    }
}
