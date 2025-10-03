package nus.edu.u.system.service.task.action;

import nus.edu.u.system.domain.dataobject.task.TaskDO;
import nus.edu.u.system.enums.task.TaskActionEnum;

/**
 * task action strategy class
 *
 * @author Lu Shuwen
 * @date 2025-10-02
 */
public interface TaskStrategy {

    boolean execute(TaskDO task, Long targetUserId, Object... params);

    TaskActionEnum getType();
}
