package nus.edu.u.system.service.task.action.strategy;

import nus.edu.u.system.domain.dataobject.task.TaskDO;
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
    public boolean execute(TaskDO task, Long targetUserId, Object... params) {
        boolean isSuccess = taskMapper.deleteById(task.getId()) > 0;
        return isSuccess
                && taskLogService.insertTaskLog(task.getId(), targetUserId, getType().getCode());
    }
}
