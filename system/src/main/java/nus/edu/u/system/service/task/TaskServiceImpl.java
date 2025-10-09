package nus.edu.u.system.service.task;

import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.*;
import static nus.edu.u.system.enums.task.TaskActionEnum.getUpdateTaskAction;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import nus.edu.u.system.domain.vo.task.TasksRespVO;
import nus.edu.u.system.enums.task.TaskActionEnum;
import nus.edu.u.system.mapper.dept.DeptMapper;
import nus.edu.u.system.mapper.task.EventMapper;
import nus.edu.u.system.mapper.task.TaskMapper;
import nus.edu.u.system.mapper.user.UserMapper;
import nus.edu.u.system.service.task.action.TaskActionFactory;
import nus.edu.u.system.service.task.builder.TaskRespVOBuilder;
import nus.edu.u.system.service.task.builder.TasksRespVOBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskServiceImpl implements TaskService {

    @Resource private TaskMapper taskMapper;

    @Resource private EventMapper eventMapper;

    @Resource private UserMapper userMapper;

    @Resource private DeptMapper deptMapper;

    @Resource private TaskActionFactory taskActionFactory;

    @Override
    @Transactional
    public TaskRespVO createTask(Long eventId, TaskCreateReqVO reqVO) {
        EventDO event = eventMapper.selectById(eventId);
        if (event == null) {
            throw exception(EVENT_NOT_FOUND);
        }

        UserDO assignee = userMapper.selectById(reqVO.getTargetUserId());
        if (assignee == null) {
            throw exception(TASK_ASSIGNEE_NOT_FOUND);
        }

        if (!Objects.equals(event.getTenantId(), assignee.getTenantId())) {
            throw exception(TASK_ASSIGNEE_TENANT_MISMATCH);
        }

        TaskDO task = TaskConvert.INSTANCE.convert(reqVO);
        task.setEventId(eventId);
        task.setTenantId(event.getTenantId());
        task.setStartTime(reqVO.getStartTime());
        task.setEndTime(reqVO.getEndTime());

        boolean isSuccess =
                taskActionFactory
                        .getStrategy(TaskActionEnum.CREATE)
                        .execute(
                                task,
                                reqVO.getTargetUserId(),
                                event.getStartTime(),
                                event.getEndTime());

        if (!isSuccess) {
            throw exception(TASK_CREATE_FAILED);
        }

        UserDO assigner = fetchUser(event.getUserId());

        return TaskRespVOBuilder.from(task)
                .withEvent(event)
                .withEventSupplier(() -> fetchEvent(task.getEventId()))
                .withAssigner(assigner)
                .withAssignerSupplier(() -> fetchUser(event.getUserId()))
                .withAssignerGroupsResolver(user -> resolveAssignerGroups(user.getDeptId()))
                .withAssignee(assignee)
                .withAssigneeSupplier(
                        () -> {
                            Long userId = task.getUserId();
                            return userId != null ? userMapper.selectById(userId) : null;
                        })
                .withAssigneeGroupsResolver(
                        user -> resolveCrudGroups(user.getDeptId(), Collections.emptyMap()))
                .build();
    }

    @Override
    @Transactional
    public TaskRespVO updateTask(Long eventId, Long taskId, TaskUpdateReqVO reqVO, Integer type) {
        EventDO event = eventMapper.selectById(eventId);
        if (event == null) {
            throw exception(EVENT_NOT_FOUND);
        }

        TaskDO task = taskMapper.selectById(taskId);
        if (task == null || !Objects.equals(task.getEventId(), eventId)) {
            throw exception(TASK_NOT_FOUND);
        }

        Long targetUserId =
                reqVO.getTargetUserId() != null ? reqVO.getTargetUserId() : task.getUserId();

        UserDO assignee = null;
        if (targetUserId != null) {
            assignee = userMapper.selectById(targetUserId);
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

        if (reqVO.getName() != null) {
            task.setName(reqVO.getName());
        }
        if (reqVO.getDescription() != null) {
            task.setDescription(reqVO.getDescription());
        }
        task.setStartTime(start);
        task.setEndTime(end);
        task.setUserId(targetUserId);
        task.setTenantId(event.getTenantId());

        if (!Arrays.asList(getUpdateTaskAction()).contains(type)) {
            throw exception(WRONG_TASK_ACTION_TYPE);
        }
        boolean isSuccess =
                taskActionFactory
                        .getStrategy(TaskActionEnum.getEnum(type))
                        .execute(task, targetUserId, event.getStartTime(), event.getEndTime());
        if (!isSuccess) {
            throw exception(UPDATE_FAILURE);
        }

        UserDO assigner = fetchUser(event.getUserId());

        return TaskRespVOBuilder.from(task)
                .withEvent(event)
                .withEventSupplier(() -> fetchEvent(task.getEventId()))
                .withAssigner(assigner)
                .withAssignerSupplier(() -> fetchUser(event.getUserId()))
                .withAssignerGroupsResolver(user -> resolveAssignerGroups(user.getDeptId()))
                .withAssignee(assignee)
                .withAssigneeSupplier(
                        () -> {
                            Long userId = task.getUserId();
                            return userId != null ? userMapper.selectById(userId) : null;
                        })
                .withAssigneeGroupsResolver(
                        user -> resolveCrudGroups(user.getDeptId(), Collections.emptyMap()))
                .build();
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
        boolean isSuccess =
                taskActionFactory.getStrategy(TaskActionEnum.DELETE).execute(task, null);
        if (!isSuccess) {
            throw exception(TASK_DELETE_FAILED);
        }
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

        UserDO assigner = fetchUser(event.getUserId());

        return TaskRespVOBuilder.from(task)
                .withEvent(event)
                .withEventSupplier(() -> fetchEvent(task.getEventId()))
                .withAssigner(assigner)
                .withAssignerSupplier(() -> fetchUser(event.getUserId()))
                .withAssignerGroupsResolver(user -> resolveAssignerGroups(user.getDeptId()))
                .withAssigneeSupplier(
                        () -> {
                            Long userId = task.getUserId();
                            return userId != null ? userMapper.selectById(userId) : null;
                        })
                .withAssigneeGroupsResolver(
                        user -> resolveCrudGroups(user.getDeptId(), Collections.emptyMap()))
                .build();
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
                buildCrudGroupsByDept(usersById);

        UserDO assigner = fetchUser(event.getUserId());

        return tasks.stream()
                .map(
                        task -> {
                            Long userId = task.getUserId();
                            UserDO user = userId != null ? usersById.get(userId) : null;
                            return TaskRespVOBuilder.from(task)
                                    .withEvent(event)
                                    .withEventSupplier(() -> fetchEvent(task.getEventId()))
                                    .withAssigner(assigner)
                                    .withAssignerSupplier(() -> fetchUser(event.getUserId()))
                                    .withAssignerGroupsResolver(
                                            assignerUser ->
                                                    resolveAssignerGroups(assignerUser.getDeptId()))
                                    .withAssignee(user)
                                    .withAssigneeSupplier(
                                            () -> {
                                                Long fallbackUserId = task.getUserId();
                                                return fallbackUserId != null
                                                        ? userMapper.selectById(fallbackUserId)
                                                        : null;
                                            })
                                    .withAssigneeGroupsResolver(
                                            assignedUser ->
                                                    resolveCrudGroups(
                                                            assignedUser.getDeptId(),
                                                            groupsByDeptId))
                                    .build();
                        })
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
                buildCrudGroupsByDept(usersById);

        List<Long> eventIds =
                tasks.stream().map(TaskDO::getEventId).filter(Objects::nonNull).distinct().toList();

        Map<Long, EventDO> eventsById =
                eventIds.isEmpty()
                        ? Map.of()
                        : eventMapper.selectBatchIds(eventIds).stream()
                                .filter(Objects::nonNull)
                                .collect(Collectors.toMap(EventDO::getId, Function.identity()));

        Map<Long, UserDO> assignersByEventId = buildAssignersByEventId(eventsById.values());

        return tasks.stream()
                .map(
                        task -> {
                            Long eventId = task.getEventId();
                            EventDO event = eventId != null ? eventsById.get(eventId) : null;
                            return TaskRespVOBuilder.from(task)
                                    .withEvent(event)
                                    .withEventSupplier(() -> fetchEvent(task.getEventId()))
                                    .withAssigner(assignersByEventId.get(eventId))
                                    .withAssignerSupplier(
                                            () -> {
                                                EventDO fallbackEvent =
                                                        fetchEvent(task.getEventId());
                                                if (fallbackEvent == null) {
                                                    return null;
                                                }
                                                Long assignerId = fallbackEvent.getUserId();
                                                return assignerId != null
                                                        ? userMapper.selectById(assignerId)
                                                        : null;
                                            })
                                    .withAssignerGroupsResolver(
                                            assignerUser ->
                                                    resolveAssignerGroups(assignerUser.getDeptId()))
                                    .withAssignee(member)
                                    .withAssigneeSupplier(
                                            () -> {
                                                Long userId = task.getUserId();
                                                return userId != null
                                                        ? userMapper.selectById(userId)
                                                        : null;
                                            })
                                    .withAssigneeGroupsResolver(
                                            assignedUser ->
                                                    resolveCrudGroups(
                                                            assignedUser.getDeptId(),
                                                            groupsByDeptId))
                                    .build();
                        })
                .toList();
    }

    private List<TasksRespVO> listDashboardTasksByMember(UserDO member) {
        List<TaskDO> tasks =
                taskMapper.selectList(
                        Wrappers.<TaskDO>lambdaQuery().eq(TaskDO::getUserId, member.getId()));

        if (tasks.isEmpty()) {
            return List.of();
        }

        Map<Long, UserDO> usersById = Map.of(member.getId(), member);
        Map<Long, List<TasksRespVO.AssignedUserVO.GroupVO>> groupsByDeptId =
                buildDashboardGroupsByDept(usersById);

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
                        task -> {
                            Long eventId = task.getEventId();
                            EventDO event = eventId != null ? eventsById.get(eventId) : null;
                            return TasksRespVOBuilder.from(task)
                                    .withEvent(event)
                                    .withEventSupplier(() -> fetchEvent(task.getEventId()))
                                    .withAssignee(member)
                                    .withAssigneeSupplier(
                                            () -> {
                                                Long userId = task.getUserId();
                                                return userId != null
                                                        ? userMapper.selectById(userId)
                                                        : null;
                                            })
                                    .withGroupResolver(
                                            assignedUser ->
                                                    resolveDashboardGroups(
                                                            assignedUser.getDeptId(),
                                                            groupsByDeptId))
                                    .build();
                        })
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
        dashboard.setTasks(listDashboardTasksByMember(member));
        return dashboard;
    }

    private EventDO fetchEvent(Long eventId) {
        if (eventId == null) {
            return null;
        }
        return eventMapper.selectById(eventId);
    }

    private UserDO fetchUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return userMapper.selectById(userId);
    }

    private Map<Long, UserDO> buildAssignersByEventId(Collection<EventDO> events) {
        if (events == null || events.isEmpty()) {
            return Map.of();
        }

        List<Long> assignerIds =
                events.stream()
                        .filter(Objects::nonNull)
                        .map(EventDO::getUserId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

        Map<Long, UserDO> assignersByUserId = fetchUsersByIds(assignerIds);
        return events.stream()
                .filter(Objects::nonNull)
                .collect(
                        Collectors.toMap(
                                EventDO::getId,
                                event -> assignersByUserId.get(event.getUserId()),
                                (existing, replacement) -> existing));
    }

    private Map<Long, UserDO> fetchUsersByIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(UserDO::getId, Function.identity()));
    }

    private List<TaskRespVO.AssignedUserVO.GroupVO> resolveCrudGroups(
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
        return List.of(toCrudGroupVO(dept));
    }

    private Map<Long, List<TaskRespVO.AssignedUserVO.GroupVO>> buildCrudGroupsByDept(
            Map<Long, UserDO> usersById) {
        if (usersById == null || usersById.isEmpty()) {
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
                .collect(Collectors.toMap(DeptDO::getId, dept -> List.of(toCrudGroupVO(dept))));
    }

    private TaskRespVO.AssignedUserVO.GroupVO toCrudGroupVO(DeptDO dept) {
        TaskRespVO.AssignedUserVO.GroupVO groupVO = new TaskRespVO.AssignedUserVO.GroupVO();
        groupVO.setId(dept.getId());
        groupVO.setName(dept.getName());
        return groupVO;
    }

    private List<TaskRespVO.AssignerUserVO.GroupVO> resolveAssignerGroups(Long deptId) {
        if (deptId == null) {
            return List.of();
        }
        DeptDO dept = deptMapper.selectById(deptId);
        if (dept == null) {
            return List.of();
        }
        TaskRespVO.AssignerUserVO.GroupVO groupVO = new TaskRespVO.AssignerUserVO.GroupVO();
        groupVO.setId(dept.getId());
        groupVO.setName(dept.getName());
        return List.of(groupVO);
    }

    private List<TasksRespVO.AssignedUserVO.GroupVO> resolveDashboardGroups(
            Long deptId, Map<Long, List<TasksRespVO.AssignedUserVO.GroupVO>> groupsByDeptId) {
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
        return List.of(toDashboardGroupVO(dept));
    }

    private Map<Long, List<TasksRespVO.AssignedUserVO.GroupVO>> buildDashboardGroupsByDept(
            Map<Long, UserDO> usersById) {
        if (usersById == null || usersById.isEmpty()) {
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
                .collect(
                        Collectors.toMap(DeptDO::getId, dept -> List.of(toDashboardGroupVO(dept))));
    }

    private TasksRespVO.AssignedUserVO.GroupVO toDashboardGroupVO(DeptDO dept) {
        TasksRespVO.AssignedUserVO.GroupVO groupVO = new TasksRespVO.AssignedUserVO.GroupVO();
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
