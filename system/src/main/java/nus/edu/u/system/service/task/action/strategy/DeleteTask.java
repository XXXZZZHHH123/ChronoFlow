package nus.edu.u.system.service.task.action.strategy;

import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.TASK_DELETE_FAILED;

import nus.edu.u.system.domain.dataobject.task.TaskDO;
import nus.edu.u.system.domain.dto.TaskActionDTO;
import nus.edu.u.system.enums.task.TaskActionEnum;
import nus.edu.u.system.service.task.action.AbstractTaskStrategy;
import org.springframework.stereotype.Component;

/**
 * @author Lu Shuwen
 * @date 2025-10-02
 */
@Component
public class DeleteTask extends AbstractTaskStrategy {

    @Override
    public TaskActionEnum getType() {
        return TaskActionEnum.DELETE;
    }

    @Override
    public void execute(TaskDO task, TaskActionDTO actionDTO, Object... params) {
        boolean isSuccess = taskMapper.deleteById(task.getId()) > 0;
        if (!isSuccess) {
            throw exception(TASK_DELETE_FAILED);
        }
        taskLogService.insertTaskLog(task.getId(), null, getType().getCode());
    }
}
