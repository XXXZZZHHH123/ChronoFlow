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
import static nus.edu.u.system.enums.ErrorCodeConstants.*;

/**
 * @author Lu Shuwen
 * @date 2025-10-03
 */
@Component
public class BlockTask extends AbstractTaskStrategy {

    @Override
    public TaskActionEnum getType() {
        return TaskActionEnum.BLOCK;
    }

    @Override
    public void execute(TaskDO task, TaskActionDTO actionDTO, Object... params) {
        validateTimeRange(
                task,
                task.getStartTime(),
                task.getEndTime(),
                actionDTO.getEventStartTime(),
                actionDTO.getEventEndTime());
        if (!ObjectUtil.equals(task.getStatus(), TaskStatusEnum.PROGRESS.getStatus())) {
            throw exception(MODIFY_WRONG_TASK_STATUS, getType().getAction(), TaskStatusEnum.getEnum(task.getStatus()));
        }
        task.setStatus(TaskStatusEnum.BLOCKED.getStatus());
        task.setUserId(null);
        boolean isSuccess = taskMapper.updateById(task) > 0;
        if (!isSuccess) {
            throw exception(BLOCK_TASK_FAILED);
        }
        Long taskLogId = taskLogService.insertTaskLog(task.getId(), null, getType().getCode());
        uploadFiles(taskLogId, task.getEventId(), actionDTO.getFiles());
    }
}
