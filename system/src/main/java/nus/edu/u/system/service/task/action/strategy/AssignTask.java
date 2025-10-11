package nus.edu.u.system.service.task.action.strategy;

import static nus.edu.u.common.constant.PermissionConstants.ASSIGN_TASK;
import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.ASSIGN_TASK_FAILED;

import cn.dev33.satoken.stp.StpUtil;
import nus.edu.u.system.domain.dataobject.task.TaskDO;
import nus.edu.u.system.domain.dto.TaskActionDTO;
import nus.edu.u.system.enums.task.TaskActionEnum;
import nus.edu.u.system.enums.task.TaskStatusEnum;
import nus.edu.u.system.service.task.action.AbstractTaskStrategy;
import org.springframework.stereotype.Component;

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
        StpUtil.checkPermission(ASSIGN_TASK);
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
        Long taskLogId =
                taskLogService.insertTaskLog(
                        task.getId(), actionDTO.getTargetUserId(), getType().getCode(), actionDTO.getRemark());
        uploadFiles(taskLogId, task.getEventId(), actionDTO.getFiles());
    }
}
