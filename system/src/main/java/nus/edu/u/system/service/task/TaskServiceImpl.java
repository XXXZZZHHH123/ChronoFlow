package nus.edu.u.system.service.task;

import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.*;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import nus.edu.u.system.convert.task.TaskConvert;
import nus.edu.u.system.domain.dataobject.dept.DeptDO;
import nus.edu.u.system.domain.dataobject.task.EventDO;
import nus.edu.u.system.domain.dataobject.task.TaskDO;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.vo.task.TaskCreateReqVO;
import nus.edu.u.system.domain.vo.task.TaskRespVO;
import nus.edu.u.system.domain.vo.task.TaskUpdateReqVO;
import nus.edu.u.system.enums.task.TaskStatusEnum;
import nus.edu.u.system.mapper.dept.DeptMapper;
import nus.edu.u.system.mapper.task.EventMapper;
import nus.edu.u.system.mapper.task.TaskMapper;
import nus.edu.u.system.mapper.user.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskServiceImpl implements TaskService {

    @Resource private TaskMapper taskMapper;
    @Resource private EventMapper eventMapper;
    @Resource private UserMapper userMapper;
    @Resource private DeptMapper deptMapper;

    @Override
    @Transactional
    public TaskRespVO createTask(Long eventId, TaskCreateReqVO reqVO) {
        EventDO event = eventMapper.selectById(eventId);
        if (event == null) {
            throw exception(EVENT_NOT_FOUND);
        }

        UserDO assignee = userMapper.selectById(reqVO.getAssignedUserId());
        if (assignee == null) {
            throw exception(TASK_ASSIGNEE_NOT_FOUND);
        }

        if (!Objects.equals(event.getTenantId(), assignee.getTenantId())) {
            throw exception(TASK_ASSIGNEE_TENANT_MISMATCH);
        }

        LocalDateTime start = reqVO.getStartTime();
        LocalDateTime end = reqVO.getEndTime();
        validateTimeRange(start, end);

        TaskStatusEnum statusEnum = TaskStatusEnum.fromStatusOrDefault(reqVO.getStatus());
        if (statusEnum == null) {
            throw exception(TASK_STATUS_INVALID);
        }

        TaskDO task = TaskConvert.INSTANCE.convert(reqVO);
        task.setEventId(eventId);
        task.setTenantId(event.getTenantId());
        task.setStatus(statusEnum.getStatus());
        task.setStartTime(start);
        task.setEndTime(end);
        taskMapper.insert(task);

        TaskRespVO resp = TaskConvert.INSTANCE.toRespVO(task);
        TaskRespVO.AssignedUserVO assignedUserVO = new TaskRespVO.AssignedUserVO();
        assignedUserVO.setId(assignee.getId());
        assignedUserVO.setName(assignee.getUsername());
        assignedUserVO.setGroups(resolveGroups(assignee.getDeptId()));
        resp.setAssignedUser(assignedUserVO);
        return resp;
    }

    @Override
    @Transactional
    public TaskRespVO updateTask(Long eventId, Long taskId, TaskUpdateReqVO reqVO) {
        EventDO event = eventMapper.selectById(eventId);
        if (event == null) {
            throw exception(EVENT_NOT_FOUND);
        }

        TaskDO task = taskMapper.selectById(taskId);
        if (task == null || !Objects.equals(task.getEventId(), eventId)) {
            throw exception(TASK_NOT_FOUND);
        }

        UserDO assignee = userMapper.selectById(reqVO.getAssignedUserId());
        if (assignee == null) {
            throw exception(TASK_ASSIGNEE_NOT_FOUND);
        }

        if (!Objects.equals(event.getTenantId(), assignee.getTenantId())) {
            throw exception(TASK_ASSIGNEE_TENANT_MISMATCH);
        }

        LocalDateTime start = reqVO.getStartTime();
        LocalDateTime end = reqVO.getEndTime();
        validateTimeRange(start, end);

        TaskStatusEnum statusEnum = TaskStatusEnum.fromStatusOrDefault(reqVO.getStatus());
        if (statusEnum == null) {
            throw exception(TASK_STATUS_INVALID);
        }

        task.setName(reqVO.getName());
        task.setDescription(reqVO.getDescription());
        task.setStatus(statusEnum.getStatus());
        task.setStartTime(start);
        task.setEndTime(end);
        task.setUserId(reqVO.getAssignedUserId());
        task.setTenantId(event.getTenantId());
        taskMapper.updateById(task);

        TaskRespVO resp = TaskConvert.INSTANCE.toRespVO(task);
        TaskRespVO.AssignedUserVO assignedUserVO = new TaskRespVO.AssignedUserVO();
        assignedUserVO.setId(assignee.getId());
        assignedUserVO.setName(assignee.getUsername());
        assignedUserVO.setGroups(resolveGroups(assignee.getDeptId()));
        resp.setAssignedUser(assignedUserVO);
        return resp;
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && !start.isBefore(end)) {
            throw exception(TASK_TIME_RANGE_INVALID);
        }
    }

    private List<TaskRespVO.AssignedUserVO.GroupVO> resolveGroups(Long deptId) {
        if (deptId == null) {
            return List.of();
        }

        DeptDO dept = deptMapper.selectById(deptId);
        if (dept == null) {
            return List.of();
        }

        TaskRespVO.AssignedUserVO.GroupVO group = new TaskRespVO.AssignedUserVO.GroupVO();
        group.setId(dept.getId());
        group.setName(dept.getName());
        return List.of(group);
    }
}
