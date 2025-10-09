package nus.edu.u.system.service.task.action.strategy;

import cn.dev33.satoken.stp.StpUtil;
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
public class AcceptTask extends AbstractTaskStrategy {

    @Override
    public TaskActionEnum getType() {
        return TaskActionEnum.ACCEPT;
    }

    @Override
    public void execute(TaskDO task, TaskActionDTO taskActionDTO, Object... params) {
        validateTimeRange(
                task,
                taskActionDTO.getStartTime(),
                taskActionDTO.getEndTime(),
                taskActionDTO.getEventStartTime(),
                taskActionDTO.getEventEndTime());
        Long currentUserId = Long.parseLong(StpUtil.getLoginId().toString());
        if (!ObjectUtil.equals(currentUserId, task.getUserId())) {
            throw exception(MODIFY_OTHER_TASK_ERROR);
        }
        if (!ObjectUtil.equals(task.getStatus(), TaskStatusEnum.PENDING.getStatus())) {
            throw exception(MODIFY_WRONG_TASK_STATUS, getType().getAction(), TaskStatusEnum.getEnum(task.getStatus()));
        }
        task.setStatus(TaskStatusEnum.PROGRESS.getStatus());
        boolean isSuccess = taskMapper.updateById(task) > 0;
        if (!isSuccess) {
            throw exception(ACCEPT_TASK_FAILED);
        }
        taskLogService.insertTaskLog(task.getId(), null, getType().getCode());
    }
}
