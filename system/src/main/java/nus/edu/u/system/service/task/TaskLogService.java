package nus.edu.u.system.service.task;

import java.util.List;
import nus.edu.u.system.domain.vo.task.TaskLogRespVO;

/**
 * @author Lu Shuwen
 * @date 2025-10-02
 */
public interface TaskLogService {

    Long insertTaskLog(Long taskId, Long targetUserId, Integer action, String remark);

    List<TaskLogRespVO> getTaskLog(Long taskId);
}
