package nus.edu.u.system.service.task.action.strategy;

import java.time.LocalDateTime;
import java.util.List;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import jodd.util.Task;
import nus.edu.u.system.domain.dataobject.task.TaskDO;
import nus.edu.u.system.domain.dto.TaskActionDTO;
import nus.edu.u.system.enums.task.TaskActionEnum;
import nus.edu.u.system.enums.task.TaskStatusEnum;
import nus.edu.u.system.service.task.action.AbstractTaskStrategy;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import static nus.edu.u.common.constant.PermissionConstants.UPDATE_TASK;
import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.*;

/**
 * @author Lu Shuwen
 * @date 2025-10-03
 */
@Component
public class ApproveTask extends AbstractTaskStrategy {

    @Override
    public TaskActionEnum getType() {
        return TaskActionEnum.APPROVE;
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
        if (!ObjectUtil.equals(task.getCreator(), StpUtil.getLoginId().toString())) {
            throw exception(MODIFY_OTHER_TASK_ERROR);
        }
        if (!ObjectUtil.equals(task.getStatus(), TaskStatusEnum.PENDING_APPROVAL.getStatus())) {
            throw exception(MODIFY_WRONG_TASK_STATUS, getType().getAction(), TaskStatusEnum.getEnum(task.getStatus()));
        }
        task.setStatus(TaskStatusEnum.COMPLETED.getStatus());
        task.setUserId(null);
        boolean isSuccess = taskMapper.updateById(task) > 0;
        if (!isSuccess) {
            throw exception(APPROVE_TASK_FAILED);
        }
        taskLogService.insertTaskLog(task.getId(), null, getType().getCode());
    }
}
