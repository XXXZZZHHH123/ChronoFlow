package nus.edu.u.system.service.task;

import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.*;
import static nus.edu.u.system.enums.task.TaskActionEnum.getUpdateTaskAction;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import nus.edu.u.system.domain.dataobject.user.UserGroupDO;
import nus.edu.u.system.domain.dto.TaskActionDTO;
import nus.edu.u.system.domain.vo.task.TaskCreateReqVO;
import nus.edu.u.system.domain.vo.task.TaskDashboardRespVO;
import nus.edu.u.system.domain.vo.task.TaskRespVO;
import nus.edu.u.system.domain.vo.task.TaskUpdateReqVO;
import nus.edu.u.system.domain.vo.task.TasksRespVO;
import nus.edu.u.system.enums.task.TaskActionEnum;
import nus.edu.u.system.mapper.dept.DeptMapper;
import nus.edu.u.system.mapper.task.EventMapper;
import nus.edu.u.system.mapper.task.TaskMapper;
import nus.edu.u.system.mapper.user.UserGroupMapper;
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

    @Resource private UserGroupMapper userGroupMapper;

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

        TaskActionDTO actionDTO =
                TaskActionDTO.builder()
                        .startTime(reqVO.getStartTime())
                        .endTime(reqVO.getEndTime())
                        .files(reqVO.getFiles())
                        .targetUserId(reqVO.getTargetUserId())
                        .eventStartTime(event.getStartTime())
                        .eventEndTime(event.getEndTime())
                        .build();
        taskActionFactory.getStrategy(TaskActionEnum.CREATE).execute(task, actionDTO);

        UserDO assigner = fetchUser(event.getUserId());

        return TaskRespVOBuilder.from(task)
                .withEvent(event)
                .withEventSupplier(() -> fetchEvent(task.getEventId()))
                .withAssigner(assigner)
                .withAssignerSupplier(() -> fetchUser(event.getUserId()))
                .withAssignerGroupsResolver(
                        user -> resolveAssignerGroups(user.getId(), event.getId()))
                .withAssignee(assignee)
                .withAssigneeSupplier(
                        () -> {
                            Long userId = task.getUserId();
                            return userId != null ? userMapper.selectById(userId) : null;
                        })
                .withAssigneeGroupsResolver(
                        user -> resolveCrudGroups(user.getId(), event.getId(), null))
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

        UserDO assignee = null;
        if (reqVO.getTargetUserId() != null) {
            assignee = userMapper.selectById(reqVO.getTargetUserId());
            if (assignee == null) {
                throw exception(TASK_ASSIGNEE_NOT_FOUND);
            }

            if (!Objects.equals(event.getTenantId(), assignee.getTenantId())) {
                throw exception(TASK_ASSIGNEE_TENANT_MISMATCH);
            }
        }

        if (!Arrays.asList(getUpdateTaskAction()).contains(type)) {
            throw exception(WRONG_TASK_ACTION_TYPE);
        }
        TaskActionDTO actionDTO =
                TaskActionDTO.builder()
                        .name(reqVO.getName())
                        .description(reqVO.getDescription())
                        .startTime(reqVO.getStartTime())
                        .endTime(reqVO.getEndTime())
                        .eventStartTime(event.getStartTime())
                        .eventEndTime(event.getEndTime())
                        .targetUserId(reqVO.getTargetUserId())
                        .files(reqVO.getFiles())
                        .remark(reqVO.getRemark())
                        .build();

        taskActionFactory.getStrategy(TaskActionEnum.getEnum(type)).execute(task, actionDTO);

        UserDO assigner = fetchUser(event.getUserId());

        return TaskRespVOBuilder.from(task)
                .withEvent(event)
                .withEventSupplier(() -> fetchEvent(task.getEventId()))
                .withAssigner(assigner)
                .withAssignerSupplier(() -> fetchUser(event.getUserId()))
                .withAssignerGroupsResolver(
                        user -> resolveAssignerGroups(user.getId(), event.getId()))
                .withAssignee(assignee)
                .withAssigneeSupplier(
                        () -> {
                            Long userId = task.getUserId();
                            return userId != null ? userMapper.selectById(userId) : null;
                        })
                .withAssigneeGroupsResolver(
                        user -> resolveCrudGroups(user.getId(), event.getId(), null))
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
        taskActionFactory.getStrategy(TaskActionEnum.DELETE).execute(task, null);
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
                .withAssignerGroupsResolver(
                        user -> resolveAssignerGroups(user.getId(), event.getId()))
                .withAssigneeSupplier(
                        () -> {
                            Long userId = task.getUserId();
                            return userId != null ? userMapper.selectById(userId) : null;
                        })
                .withAssigneeGroupsResolver(
                        user -> resolveCrudGroups(user.getId(), event.getId(), null))
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

        Map<Long, List<TaskRespVO.AssignedUserVO.GroupVO>> groupsByUserId =
                buildCrudGroupsByUser(usersById.keySet(), eventId);

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
                                                    resolveAssignerGroups(
                                                            assignerUser.getId(), eventId))
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
                                                            assignedUser.getId(),
                                                            eventId,
                                                            groupsByUserId))
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
        Map<Long, Map<Long, List<TaskRespVO.AssignedUserVO.GroupVO>>> crudGroupsCache =
                new LinkedHashMap<>();
        Map<Long, Map<Long, List<TaskRespVO.AssignerUserVO.GroupVO>>> assignerGroupsCache =
                new LinkedHashMap<>();

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
                            Long currentEventId = eventId;
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
                                                    resolveAssignerGroupsWithCache(
                                                            assignerUser,
                                                            currentEventId,
                                                            assignerGroupsCache))
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
                                                    resolveCrudGroupsWithCache(
                                                            assignedUser,
                                                            currentEventId,
                                                            crudGroupsCache,
                                                            usersById.keySet()))
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
        Collection<Long> memberIds = usersById.keySet();
        Map<Long, Map<Long, List<TasksRespVO.AssignedUserVO.GroupVO>>> dashboardGroupsCache =
                new LinkedHashMap<>();

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
                            Long currentEventId = eventId;
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
                                                    resolveDashboardGroupsWithCache(
                                                            assignedUser,
                                                            currentEventId,
                                                            dashboardGroupsCache,
                                                            memberIds))
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

    private Map<Long, List<DeptDO>> fetchUserDeptsByEvent(Collection<Long> userIds, Long eventId) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        var query = Wrappers.<UserGroupDO>lambdaQuery().in(UserGroupDO::getUserId, userIds);
        if (eventId != null) {
            query.eq(UserGroupDO::getEventId, eventId);
        }

        List<UserGroupDO> relations = userGroupMapper.selectList(query);
        if (relations == null || relations.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<Long>> deptIdsByUserId = new LinkedHashMap<>();
        for (UserGroupDO relation : relations) {
            if (relation == null || relation.getUserId() == null || relation.getDeptId() == null) {
                continue;
            }
            deptIdsByUserId
                    .computeIfAbsent(relation.getUserId(), ignored -> new ArrayList<>())
                    .add(relation.getDeptId());
        }

        if (deptIdsByUserId.isEmpty()) {
            return Map.of();
        }

        LinkedHashSet<Long> deptIds =
                deptIdsByUserId.values().stream()
                        .flatMap(Collection::stream)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        if (deptIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, DeptDO> deptsById =
                deptMapper.selectBatchIds(deptIds).stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(DeptDO::getId, Function.identity()));

        Map<Long, List<DeptDO>> result = new LinkedHashMap<>();
        for (Map.Entry<Long, List<Long>> entry : deptIdsByUserId.entrySet()) {
            List<DeptDO> depts =
                    entry.getValue().stream()
                            .map(deptsById::get)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
            if (!depts.isEmpty()) {
                result.put(entry.getKey(), depts);
            }
        }
        return result;
    }

    private List<DeptDO> fetchUserDeptsByEvent(Long userId, Long eventId) {
        if (userId == null) {
            return List.of();
        }
        return fetchUserDeptsByEvent(List.of(userId), eventId).getOrDefault(userId, List.of());
    }

    private Map<Long, List<TaskRespVO.AssignedUserVO.GroupVO>> buildCrudGroupsByUser(
            Collection<Long> userIds, Long eventId) {
        if (userIds == null || userIds.isEmpty() || eventId == null) {
            return Map.of();
        }
        Map<Long, List<DeptDO>> deptsByUserId = fetchUserDeptsByEvent(userIds, eventId);
        if (deptsByUserId.isEmpty()) {
            return Map.of();
        }
        return deptsByUserId.entrySet().stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                entry ->
                                        entry.getValue().stream()
                                                .map(this::toCrudGroupVO)
                                                .collect(Collectors.toList())));
    }

    private List<TaskRespVO.AssignedUserVO.GroupVO> resolveCrudGroups(
            Long userId,
            Long eventId,
            Map<Long, List<TaskRespVO.AssignedUserVO.GroupVO>> groupsByUserId) {
        if (userId == null || eventId == null) {
            return List.of();
        }
        if (groupsByUserId != null) {
            if (groupsByUserId.containsKey(userId)) {
                return groupsByUserId.getOrDefault(userId, List.of());
            }
            return List.of();
        }
        return fetchUserDeptsByEvent(userId, eventId).stream()
                .map(this::toCrudGroupVO)
                .collect(Collectors.toList());
    }

    private List<TaskRespVO.AssignedUserVO.GroupVO> resolveCrudGroups(Long userId, Long eventId) {
        return resolveCrudGroups(userId, eventId, null);
    }

    private TaskRespVO.AssignedUserVO.GroupVO toCrudGroupVO(DeptDO dept) {
        TaskRespVO.AssignedUserVO.GroupVO groupVO = new TaskRespVO.AssignedUserVO.GroupVO();
        groupVO.setId(dept.getId());
        groupVO.setName(dept.getName());
        return groupVO;
    }

    private List<TaskRespVO.AssignerUserVO.GroupVO> resolveAssignerGroups(
            Long userId, Long eventId) {
        if (userId == null || eventId == null) {
            return List.of();
        }
        return fetchUserDeptsByEvent(userId, eventId).stream()
                .map(
                        dept -> {
                            TaskRespVO.AssignerUserVO.GroupVO groupVO =
                                    new TaskRespVO.AssignerUserVO.GroupVO();
                            groupVO.setId(dept.getId());
                            groupVO.setName(dept.getName());
                            return groupVO;
                        })
                .collect(Collectors.toList());
    }

    private List<TaskRespVO.AssignerUserVO.GroupVO> resolveAssignerGroupsWithCache(
            UserDO user,
            Long eventId,
            Map<Long, Map<Long, List<TaskRespVO.AssignerUserVO.GroupVO>>> cache) {
        if (user == null || user.getId() == null || eventId == null) {
            return List.of();
        }
        Map<Long, List<TaskRespVO.AssignerUserVO.GroupVO>> groupsForEvent =
                cache.computeIfAbsent(eventId, ignored -> new LinkedHashMap<>());
        return groupsForEvent.computeIfAbsent(
                user.getId(), ignored -> resolveAssignerGroups(user.getId(), eventId));
    }

    private List<TaskRespVO.AssignedUserVO.GroupVO> resolveCrudGroupsWithCache(
            UserDO user,
            Long eventId,
            Map<Long, Map<Long, List<TaskRespVO.AssignedUserVO.GroupVO>>> cache,
            Collection<Long> usersToPrefetch) {
        if (user == null || user.getId() == null || eventId == null) {
            return List.of();
        }
        Map<Long, List<TaskRespVO.AssignedUserVO.GroupVO>> groupsForEvent =
                cache.computeIfAbsent(
                        eventId, ignored -> buildCrudGroupsByUser(usersToPrefetch, eventId));
        return groupsForEvent.getOrDefault(user.getId(), List.of());
    }

    private Map<Long, List<TasksRespVO.AssignedUserVO.GroupVO>> buildDashboardGroupsByUser(
            Collection<Long> userIds, Long eventId) {
        if (userIds == null || userIds.isEmpty() || eventId == null) {
            return Map.of();
        }
        Map<Long, List<DeptDO>> deptsByUserId = fetchUserDeptsByEvent(userIds, eventId);
        if (deptsByUserId.isEmpty()) {
            return Map.of();
        }
        return deptsByUserId.entrySet().stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                entry ->
                                        entry.getValue().stream()
                                                .map(this::toDashboardGroupVO)
                                                .collect(Collectors.toList())));
    }

    private List<TasksRespVO.AssignedUserVO.GroupVO> resolveDashboardGroups(
            Long userId,
            Long eventId,
            Map<Long, List<TasksRespVO.AssignedUserVO.GroupVO>> groupsByUserId) {
        if (userId == null || eventId == null) {
            return List.of();
        }
        if (groupsByUserId != null) {
            if (groupsByUserId.containsKey(userId)) {
                return groupsByUserId.getOrDefault(userId, List.of());
            }
            return List.of();
        }
        return fetchUserDeptsByEvent(userId, eventId).stream()
                .map(this::toDashboardGroupVO)
                .collect(Collectors.toList());
    }

    private List<TasksRespVO.AssignedUserVO.GroupVO> resolveDashboardGroups(
            Long userId, Long eventId) {
        return resolveDashboardGroups(userId, eventId, null);
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

    private List<TasksRespVO.AssignedUserVO.GroupVO> resolveDashboardGroupsWithCache(
            UserDO user,
            Long eventId,
            Map<Long, Map<Long, List<TasksRespVO.AssignedUserVO.GroupVO>>> cache,
            Collection<Long> usersToPrefetch) {
        if (user == null || user.getId() == null || eventId == null) {
            return List.of();
        }
        Map<Long, List<TasksRespVO.AssignedUserVO.GroupVO>> groupsForEvent =
                cache.computeIfAbsent(
                        eventId, ignored -> buildDashboardGroupsByUser(usersToPrefetch, eventId));
        return groupsForEvent.getOrDefault(user.getId(), List.of());
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
        if (member == null || member.getId() == null) {
            return List.of();
        }

        return fetchUserDeptsByEvent(member.getId(), null).stream()
                .map(
                        dept -> {
                            TaskDashboardRespVO.GroupVO groupVO = new TaskDashboardRespVO.GroupVO();
                            groupVO.setId(dept.getId());
                            groupVO.setName(dept.getName());
                            groupVO.setSort(dept.getSort());
                            groupVO.setLeadUserId(dept.getLeadUserId());
                            groupVO.setRemark(dept.getRemark());
                            groupVO.setStatus(dept.getStatus());
                            groupVO.setEvent(toGroupEvent(dept.getEventId()));
                            return groupVO;
                        })
                .collect(Collectors.toList());
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
