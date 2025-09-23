package nus.edu.u.system.service.task;

import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.*;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
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
        validateTaskWithinEventRange(start, end, event);

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

        return buildTaskResponse(task, assignee);
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

        Long assigneeId =
                reqVO.getAssignedUserId() != null ? reqVO.getAssignedUserId() : task.getUserId();

        UserDO assignee = null;
        if (assigneeId != null) {
            assignee = userMapper.selectById(assigneeId);
            if (assignee == null) {
                throw exception(TASK_ASSIGNEE_NOT_FOUND);
            }

            if (!Objects.equals(event.getTenantId(), assignee.getTenantId())) {
                throw exception(TASK_ASSIGNEE_TENANT_MISMATCH);
            }
        }

        LocalDateTime start =
                reqVO.getStartTime() != null ? reqVO.getStartTime() : task.getStartTime();
        LocalDateTime end = reqVO.getEndTime() != null ? reqVO.getEndTime() : task.getEndTime();
        validateTimeRange(start, end);
        validateTaskWithinEventRange(start, end, event);

        Integer status = task.getStatus();
        if (reqVO.getStatus() != null) {
            TaskStatusEnum statusEnum = TaskStatusEnum.fromStatusOrDefault(reqVO.getStatus());
            if (statusEnum == null) {
                throw exception(TASK_STATUS_INVALID);
            }
            status = statusEnum.getStatus();
        }

        if (reqVO.getName() != null) {
            task.setName(reqVO.getName());
        }
        if (reqVO.getDescription() != null) {
            task.setDescription(reqVO.getDescription());
        }
        task.setStatus(status);
        task.setStartTime(start);
        task.setEndTime(end);
        task.setUserId(assigneeId);
        task.setTenantId(event.getTenantId());
        taskMapper.updateById(task);

        return buildTaskResponse(task, assignee);
    }

    @Override
    @Transactional
    public void deleteTask(Long eventId, Long taskId) {
        EventDO event = eventMapper.selectById(eventId);
        if (event == null) {
            throw exception(EVENT_NOT_FOUND);
        }

        TaskDO task = taskMapper.selectById(taskId);
        if (task == null || !Objects.equals(task.getEventId(), eventId)) {
            throw exception(TASK_NOT_FOUND);
        }

        taskMapper.deleteById(taskId);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskRespVO getTask(Long eventId, Long taskId) {
        EventDO event = eventMapper.selectById(eventId);
        if (event == null) {
            throw exception(EVENT_NOT_FOUND);
        }

        TaskDO task = taskMapper.selectById(taskId);
        if (task == null || !Objects.equals(task.getEventId(), eventId)) {
            throw exception(TASK_NOT_FOUND);
        }

        return buildTaskResponse(task, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskRespVO> listTasksByEvent(Long eventId) {
        EventDO event = eventMapper.selectById(eventId);
        if (event == null) {
            throw exception(EVENT_NOT_FOUND);
        }

        List<TaskDO> tasks =
                taskMapper.selectList(
                        Wrappers.<TaskDO>lambdaQuery().eq(TaskDO::getEventId, eventId));

        if (tasks.isEmpty()) {
            return List.of();
        }

        List<Long> userIds =
                tasks.stream().map(TaskDO::getUserId).filter(Objects::nonNull).distinct().toList();

        Map<Long, UserDO> usersById =
                userIds.isEmpty()
                        ? Map.of()
                        : userMapper.selectBatchIds(userIds).stream()
                                .collect(Collectors.toMap(UserDO::getId, Function.identity()));

        Map<Long, List<TaskRespVO.AssignedUserVO.GroupVO>> groupsByDeptId =
                buildGroupsByDept(usersById);

        return tasks.stream()
                .map(
                        task ->
                                buildTaskResponse(
                                        task, usersById.get(task.getUserId()), groupsByDeptId))
                .toList();
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && !start.isBefore(end)) {
            throw exception(TASK_TIME_RANGE_INVALID);
        }
    }

    private void validateTaskWithinEventRange(
            LocalDateTime taskStart, LocalDateTime taskEnd, EventDO event) {
        LocalDateTime eventStart = event.getStartTime();
        LocalDateTime eventEnd = event.getEndTime();

        if (taskStart != null && eventStart != null && taskStart.isBefore(eventStart)) {
            throw exception(TASK_TIME_OUTSIDE_EVENT);
        }

        if (taskEnd != null && eventEnd != null && taskEnd.isAfter(eventEnd)) {
            throw exception(TASK_TIME_OUTSIDE_EVENT);
        }
    }

    private TaskRespVO buildTaskResponse(TaskDO task, UserDO assignee) {
        return buildTaskResponse(task, assignee, null);
    }

    private TaskRespVO buildTaskResponse(
            TaskDO task,
            UserDO assignee,
            Map<Long, List<TaskRespVO.AssignedUserVO.GroupVO>> groupsByDeptId) {
        TaskRespVO resp = TaskConvert.INSTANCE.toRespVO(task);
        UserDO user = assignee;

        if (user == null && task.getUserId() != null) {
            user = userMapper.selectById(task.getUserId());
        }

        if (user != null) {
            TaskRespVO.AssignedUserVO assignedUserVO = new TaskRespVO.AssignedUserVO();
            assignedUserVO.setId(user.getId());
            assignedUserVO.setName(user.getUsername());
            assignedUserVO.setGroups(resolveGroups(user.getDeptId(), groupsByDeptId));
            resp.setAssignedUser(assignedUserVO);
        } else {
            resp.setAssignedUser(null);
        }

        return resp;
    }

    private List<TaskRespVO.AssignedUserVO.GroupVO> resolveGroups(
            Long deptId, Map<Long, List<TaskRespVO.AssignedUserVO.GroupVO>> groupsByDeptId) {
        if (deptId == null) {
            return List.of();
        }

        if (groupsByDeptId != null && groupsByDeptId.containsKey(deptId)) {
            return groupsByDeptId.get(deptId);
        }

        DeptDO dept = deptMapper.selectById(deptId);
        if (dept == null) {
            return List.of();
        }

        return List.of(toGroupVO(dept));
    }

    private Map<Long, List<TaskRespVO.AssignedUserVO.GroupVO>> buildGroupsByDept(
            Map<Long, UserDO> usersById) {
        if (usersById.isEmpty()) {
            return Map.of();
        }

        List<Long> deptIds =
                usersById.values().stream()
                        .map(UserDO::getDeptId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

        if (deptIds.isEmpty()) {
            return Map.of();
        }

        return deptMapper.selectBatchIds(deptIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(DeptDO::getId, dept -> List.of(toGroupVO(dept))));
    }

    private TaskRespVO.AssignedUserVO.GroupVO toGroupVO(DeptDO dept) {
        TaskRespVO.AssignedUserVO.GroupVO groupVO = new TaskRespVO.AssignedUserVO.GroupVO();
        groupVO.setId(dept.getId());
        groupVO.setName(dept.getName());
        return groupVO;
    }
}
