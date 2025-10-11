package nus.edu.u.system.service.task.action.strategy;

import static nus.edu.u.common.constant.PermissionConstants.UPDATE_TASK;
import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.MODIFY_WRONG_TASK_STATUS;
import static nus.edu.u.system.enums.ErrorCodeConstants.TASK_UPDATE_FAILED;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import nus.edu.u.system.domain.dataobject.task.TaskDO;
import nus.edu.u.system.domain.dto.TaskActionDTO;
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
    public void execute(TaskDO task, TaskActionDTO actionDTO, Object... params) {
        StpUtil.checkPermission(UPDATE_TASK);
        validateTimeRange(
                task,
                task.getStartTime(),
                task.getEndTime(),
                actionDTO.getEventStartTime(),
                actionDTO.getEventEndTime());
        if (ObjectUtil.equals(task.getStatus(), TaskStatusEnum.COMPLETED.getStatus())) {
            throw exception(
                    MODIFY_WRONG_TASK_STATUS,
                    getType().getAction(),
                    TaskStatusEnum.getEnum(task.getStatus()));
        }
        task.setStatus(TaskStatusEnum.PENDING.getStatus());
        task.setUserId(
                ObjectUtil.isNull(actionDTO.getTargetUserId())
                        ? task.getUserId()
                        : actionDTO.getTargetUserId());
        task.setName(ObjectUtil.isNull(actionDTO.getName()) ? task.getName() : actionDTO.getName());
        task.setDescription(
                ObjectUtil.isNull(actionDTO.getDescription())
                        ? task.getDescription()
                        : actionDTO.getDescription());
        task.setStartTime(
                ObjectUtil.isNull(actionDTO.getStartTime())
                        ? task.getStartTime()
                        : actionDTO.getStartTime());
        task.setEndTime(
                ObjectUtil.isNull(actionDTO.getEndTime())
                        ? task.getEndTime()
                        : actionDTO.getEndTime());
        boolean isSuccess = taskMapper.updateById(task) > 0;
        if (!isSuccess) {
            throw exception(TASK_UPDATE_FAILED);
        }
        Long taskLogId = taskLogService.insertTaskLog(task.getId(), null, getType().getCode(), actionDTO.getRemark());
        uploadFiles(taskLogId, task.getEventId(), actionDTO.getFiles());
    }
}
