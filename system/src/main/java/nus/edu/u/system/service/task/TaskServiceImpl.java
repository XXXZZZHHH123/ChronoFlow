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
import nus.edu.u.system.domain.vo.task.TaskDashboardRespVO;
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

        return buildTaskResponse(task, event, assignee);
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

        return buildTaskResponse(task, event, assignee);
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

        return buildTaskResponse(task, event, null);
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
                                        task,
                                        event,
                                        usersById.get(task.getUserId()),
                                        groupsByDeptId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskRespVO> listTasksByMember(Long memberId) {
        UserDO member = userMapper.selectById(memberId);
        if (member == null) {
            throw exception(USER_NOT_FOUND);
        }

        List<TaskDO> tasks =
                taskMapper.selectList(
                        Wrappers.<TaskDO>lambdaQuery().eq(TaskDO::getUserId, memberId));

        if (tasks.isEmpty()) {
            return List.of();
        }

        Map<Long, UserDO> usersById = Map.of(memberId, member);
        Map<Long, List<TaskRespVO.AssignedUserVO.GroupVO>> groupsByDeptId =
                buildGroupsByDept(usersById);

        List<Long> eventIds =
                tasks.stream().map(TaskDO::getEventId).filter(Objects::nonNull).distinct().toList();

        Map<Long, EventDO> eventsById =
                eventIds.isEmpty()
                        ? Map.of()
                        : eventMapper.selectBatchIds(eventIds).stream()
                                .filter(Objects::nonNull)
                                .collect(Collectors.toMap(EventDO::getId, Function.identity()));

        return tasks.stream()
                .map(
                        task ->
                                buildTaskResponse(
                                        task,
                                        eventsById.get(task.getEventId()),
                                        member,
                                        groupsByDeptId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDashboardRespVO getByMemberId(Long memberId) {
        UserDO member = userMapper.selectById(memberId);
        if (member == null) {
            throw exception(USER_NOT_FOUND);
        }

        TaskDashboardRespVO dashboard = new TaskDashboardRespVO();
        dashboard.setMember(toMemberVO(member));
        dashboard.setGroups(resolveMemberGroups(member));
        dashboard.setTasks(listTasksByMember(memberId));
        return dashboard;
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

    private TaskRespVO buildTaskResponse(TaskDO task, EventDO event, UserDO assignee) {
        return buildTaskResponse(task, event, assignee, null);
    }

    private TaskRespVO buildTaskResponse(
            TaskDO task,
            EventDO event,
            UserDO assignee,
            Map<Long, List<TaskRespVO.AssignedUserVO.GroupVO>> groupsByDeptId) {
        TaskRespVO resp = TaskConvert.INSTANCE.toRespVO(task);
        EventDO eventData = event != null ? event : fetchEvent(task.getEventId());
        resp.setEvent(toEventSummary(eventData));
        UserDO user = assignee;

        if (user == null && task.getUserId() != null) {
            user = userMapper.selectById(task.getUserId());
        }

        if (user != null) {
            TaskRespVO.AssignedUserVO assignedUserVO = new TaskRespVO.AssignedUserVO();
            assignedUserVO.setId(user.getId());
            assignedUserVO.setName(user.getUsername());
            assignedUserVO.setEmail(user.getEmail());
            assignedUserVO.setPhone(user.getPhone());
            assignedUserVO.setGroups(resolveGroups(user.getDeptId(), groupsByDeptId));
            resp.setAssignedUser(assignedUserVO);
        } else {
            resp.setAssignedUser(null);
        }

        return resp;
    }

    private EventDO fetchEvent(Long eventId) {
        if (eventId == null) {
            return null;
        }
        return eventMapper.selectById(eventId);
    }

    private TaskRespVO.EventSummaryVO toEventSummary(EventDO event) {
        if (event == null) {
            return null;
        }

        TaskRespVO.EventSummaryVO eventVO = new TaskRespVO.EventSummaryVO();
        eventVO.setId(event.getId());
        eventVO.setName(event.getName());
        eventVO.setDescription(event.getDescription());
        eventVO.setOrganizerId(event.getUserId());
        eventVO.setLocation(event.getLocation());
        eventVO.setStatus(event.getStatus());
        eventVO.setStartTime(event.getStartTime());
        eventVO.setEndTime(event.getEndTime());
        eventVO.setRemark(event.getRemark());
        return eventVO;
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
        groupVO.setEventId(dept.getEventId());
        groupVO.setLeadUserId(dept.getLeadUserId());
        groupVO.setRemark(dept.getRemark());
        return groupVO;
    }

    private TaskDashboardRespVO.MemberVO toMemberVO(UserDO member) {
        TaskDashboardRespVO.MemberVO memberVO = new TaskDashboardRespVO.MemberVO();
        memberVO.setId(member.getId());
        memberVO.setUsername(member.getUsername());
        memberVO.setEmail(member.getEmail());
        memberVO.setPhone(member.getPhone());
        memberVO.setStatus(member.getStatus());
        memberVO.setCreateTime(member.getCreateTime());
        memberVO.setUpdateTime(member.getUpdateTime());
        return memberVO;
    }

    private List<TaskDashboardRespVO.GroupVO> resolveMemberGroups(UserDO member) {
        Long deptId = member.getDeptId();
        if (deptId == null) {
            return List.of();
        }

        DeptDO dept = deptMapper.selectById(deptId);
        if (dept == null) {
            return List.of();
        }

        TaskDashboardRespVO.GroupVO groupVO = new TaskDashboardRespVO.GroupVO();
        groupVO.setId(dept.getId());
        groupVO.setName(dept.getName());
        groupVO.setSort(dept.getSort());
        groupVO.setLeadUserId(dept.getLeadUserId());
        groupVO.setRemark(dept.getRemark());
        groupVO.setStatus(dept.getStatus());
        groupVO.setEvent(toGroupEvent(dept.getEventId()));
        return List.of(groupVO);
    }

    private TaskDashboardRespVO.GroupVO.EventVO toGroupEvent(Long eventId) {
        if (eventId == null) {
            return null;
        }

        EventDO event = eventMapper.selectById(eventId);
        if (event == null) {
            return null;
        }

        TaskDashboardRespVO.GroupVO.EventVO eventVO = new TaskDashboardRespVO.GroupVO.EventVO();
        eventVO.setId(event.getId());
        eventVO.setName(event.getName());
        eventVO.setDescription(event.getDescription());
        eventVO.setLocation(event.getLocation());
        eventVO.setStatus(event.getStatus());
        eventVO.setStartTime(event.getStartTime());
        eventVO.setEndTime(event.getEndTime());
        eventVO.setRemark(event.getRemark());
        return eventVO;
    }
}
