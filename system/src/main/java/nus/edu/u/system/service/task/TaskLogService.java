package nus.edu.u.system.service.task;

import nus.edu.u.system.domain.vo.task.TaskLogRespVO;

import java.util.List;

/**
 * @author Lu Shuwen
 * @date 2025-10-02
 */
public interface TaskLogService {

    boolean insertTaskLog(Long taskId, Long targetUserId, Integer action);

    List<TaskLogRespVO> getTaskLog(Long taskId);
}
