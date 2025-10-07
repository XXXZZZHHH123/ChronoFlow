package nus.edu.u.system.service.task.action;

import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.TASK_TIME_OUTSIDE_EVENT;
import static nus.edu.u.system.enums.ErrorCodeConstants.TASK_TIME_RANGE_INVALID;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import nus.edu.u.system.domain.dataobject.task.TaskDO;
import nus.edu.u.system.enums.task.TaskActionEnum;
import nus.edu.u.system.mapper.task.TaskMapper;
import nus.edu.u.system.service.task.TaskLogService;
import org.springframework.stereotype.Component;

/**
 * @author Lu Shuwen
 * @date 2025-10-02
 */
@Component
public abstract class AbstractTaskStrategy implements TaskStrategy {

    @Resource protected TaskMapper taskMapper;

    @Resource protected TaskLogService taskLogService;

    public abstract boolean execute(TaskDO task, Long targetUserId, Object... params);

    public abstract TaskActionEnum getType();

    public void validateTimeRange(
            LocalDateTime taskStart,
            LocalDateTime taskEnd,
            LocalDateTime eventStart,
            LocalDateTime eventEnd) {
        if (taskStart != null && taskEnd != null && !taskStart.isBefore(taskEnd)) {
            throw exception(TASK_TIME_RANGE_INVALID);
        }
        if (taskStart != null && eventStart != null && taskStart.isBefore(eventStart)) {
            throw exception(TASK_TIME_OUTSIDE_EVENT);
        }

        if (taskEnd != null && eventEnd != null && taskEnd.isAfter(eventEnd)) {
            throw exception(TASK_TIME_OUTSIDE_EVENT);
        }
    }
}
