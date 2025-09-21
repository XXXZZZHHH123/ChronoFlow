package nus.edu.u.system.service.task;

import nus.edu.u.system.domain.vo.task.TaskCreateReqVO;
import nus.edu.u.system.domain.vo.task.TaskRespVO;

public interface TaskService {
    TaskRespVO createTask(Long eventId, TaskCreateReqVO reqVO);
}
