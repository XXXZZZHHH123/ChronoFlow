package nus.edu.u.system.service.task.action.strategy;

import java.time.LocalDateTime;
import java.util.List;

import cn.hutool.core.util.ObjectUtil;
import nus.edu.u.system.domain.dataobject.task.EventDO;
import nus.edu.u.system.domain.dataobject.task.TaskDO;
import nus.edu.u.system.domain.dto.TaskActionDTO;
import nus.edu.u.system.enums.task.TaskActionEnum;
import nus.edu.u.system.enums.task.TaskStatusEnum;
import nus.edu.u.system.service.task.action.AbstractTaskStrategy;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.ASSIGN_TASK_FAILED;
import static nus.edu.u.system.enums.ErrorCodeConstants.TASK_LOG_ERROR;

/**
 * @author Lu Shuwen
 * @date 2025-10-03
 */
@Component
public class AssignTask extends AbstractTaskStrategy {

    @Override
    public TaskActionEnum getType() {
        return TaskActionEnum.ASSIGN;
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
        boolean isSuccess = taskMapper.updateById(task) > 0;
        if (!isSuccess) {
            throw exception(ASSIGN_TASK_FAILED);
        }
        Long taskLogId = taskLogService.insertTaskLog(task.getId(), actionDTO.getTargetUserId(), getType().getCode());
        uploadFiles(taskLogId, task.getEventId(), actionDTO.getFiles());
    }
}
