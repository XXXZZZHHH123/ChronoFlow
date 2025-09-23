package nus.edu.u.system.service.task;

import java.util.List;
import nus.edu.u.system.domain.vo.task.TaskCreateReqVO;
import nus.edu.u.system.domain.vo.task.TaskRespVO;
import nus.edu.u.system.domain.vo.task.TaskUpdateReqVO;

public interface TaskService {
    TaskRespVO createTask(Long eventId, TaskCreateReqVO reqVO);

    TaskRespVO updateTask(Long eventId, Long taskId, TaskUpdateReqVO reqVO);

    void deleteTask(Long eventId, Long taskId);

    TaskRespVO getTask(Long eventId, Long taskId);

    List<TaskRespVO> listTasksByEvent(Long eventId);
}
