package nus.edu.u.system.service.task.action.strategy;

import java.time.LocalDateTime;

import cn.hutool.core.util.ObjectUtil;
import nus.edu.u.system.domain.dataobject.task.TaskDO;
import nus.edu.u.system.domain.dto.TaskActionDTO;
import nus.edu.u.system.enums.task.TaskActionEnum;
import nus.edu.u.system.enums.task.TaskStatusEnum;
import nus.edu.u.system.service.task.action.AbstractTaskStrategy;
import org.springframework.stereotype.Component;

import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.TASK_CREATE_FAILED;
import static nus.edu.u.system.enums.ErrorCodeConstants.TASK_LOG_ERROR;

/**
 * @author Lu Shuwen
 * @date 2025-10-02
 */
@Component
public class CreateTask extends AbstractTaskStrategy {

    @Override
    public TaskActionEnum getType() {
        return TaskActionEnum.CREATE;
    }

    @Override
    public void execute(TaskDO task, TaskActionDTO actionDTO, Object... params) {
        validateTimeRange(
                task,
                task.getStartTime(),
                task.getEndTime(),
                actionDTO.getEventStartTime(),
                actionDTO.getEventEndTime());
        task.setStatus(TaskStatusEnum.PENDING.getStatus());
        task.setUserId(actionDTO.getTargetUserId());
        boolean isSuccess = taskMapper.insert(task) > 0;
        if (!isSuccess) {
            throw exception(TASK_CREATE_FAILED);
        }
        Long taskLogId = taskLogService.insertTaskLog(task.getId(), actionDTO.getTargetUserId(), getType().getCode());
        uploadFiles(taskLogId, task.getEventId(), actionDTO.getFiles());
    }
}
