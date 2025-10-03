package nus.edu.u.system.service.task;

/**
 * @author Lu Shuwen
 * @date 2025-10-02
 */
public interface TaskLogService {

    boolean insertTaskLog(Long taskId, Long targetUserId, Integer action);
}
