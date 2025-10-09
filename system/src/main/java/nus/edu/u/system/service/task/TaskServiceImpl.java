package nus.edu.u.system.service.task;

import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.*;
import static nus.edu.u.system.enums.task.TaskActionEnum.getUpdateTaskAction;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import nus.edu.u.system.enums.task.TaskActionEnum;
import nus.edu.u.system.mapper.dept.DeptMapper;
import nus.edu.u.system.mapper.task.EventMapper;
import nus.edu.u.system.mapper.task.TaskMapper;
import nus.edu.u.system.mapper.user.UserMapper;
import nus.edu.u.system.service.task.action.TaskActionFactory;
import nus.edu.u.system.service.task.builder.TaskRespVOBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

        Long assignerId = parseUserId(task.getCreator());
        UserDO assigner = resolveAssigner(assignerId, assignee);

        return TaskRespVOBuilder.from(task)
                .withEvent(event)
                .withAssignee(assignee)
                .withAssigneeSupplier(() -> fetchUserById(task.getUserId()))
                .withAssigner(assigner)
                .withAssignerSupplier(() -> fetchUserById(assignerId))
                .withGroupResolver(user -> resolveGroups(user.getDeptId(), null))
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

        Long assignerId = parseUserId(task.getCreator());
        UserDO assigner = resolveAssigner(assignerId, assignee);

        return TaskRespVOBuilder.from(task)
                .withEvent(event)
                .withEventSupplier(() -> fetchEvent(task.getEventId()))
                .withAssignee(assignee)
                .withAssigneeSupplier(() -> fetchUserById(task.getUserId()))
                .withAssigner(assigner)
                .withAssignerSupplier(() -> fetchUserById(assignerId))
                .withGroupResolver(user -> resolveGroups(user.getDeptId(), null))
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

        Long assignerId = parseUserId(task.getCreator());

        return TaskRespVOBuilder.from(task)
                .withEvent(event)
                .withEventSupplier(() -> fetchEvent(task.getEventId()))
                .withAssigneeSupplier(() -> fetchUserById(task.getUserId()))
                .withAssignerSupplier(() -> fetchUserById(assignerId))
                .withGroupResolver(user -> resolveGroups(user.getDeptId(), null))
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

        Set<Long> relatedUserIds = collectRelatedUserIds(tasks);

        Map<Long, UserDO> usersById =
                relatedUserIds.isEmpty()
                        ? Map.of()
                        : userMapper.selectBatchIds(relatedUserIds).stream()
                                .filter(Objects::nonNull)
                                .collect(Collectors.toMap(UserDO::getId, Function.identity()));

        Map<Long, List<TaskRespVO.AssignedUserVO.GroupVO>> groupsByDeptId =
                buildGroupsByDept(usersById);

        return tasks.stream()
                .map(
                        task -> {
                            Long userId = task.getUserId();
                            UserDO user = userId != null ? usersById.get(userId) : null;
                            Long assignerId = parseUserId(task.getCreator());
                            UserDO assigner = assignerId != null ? usersById.get(assignerId) : null;
                            return TaskRespVOBuilder.from(task)
                                    .withEvent(event)
                                    .withEventSupplier(() -> fetchEvent(task.getEventId()))
                                    .withAssignee(user)
                                    .withAssigneeSupplier(() -> fetchUserById(task.getUserId()))
                                    .withAssigner(assigner)
                                    .withAssignerSupplier(() -> fetchUserById(assignerId))
                                    .withGroupResolver(
                                            assignedUser ->
                                                    resolveGroups(
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

        Set<Long> relatedUserIds = collectRelatedUserIds(tasks);
        Set<Long> usersToLoad = new HashSet<>(relatedUserIds);
        usersToLoad.remove(memberId);

        Map<Long, UserDO> enrichedUsers =
                usersToLoad.isEmpty()
                        ? new HashMap<>()
                        : userMapper.selectBatchIds(usersToLoad).stream()
                                .filter(Objects::nonNull)
                                .collect(
                                        Collectors.toMap(
                                                UserDO::getId,
                                                Function.identity(),
                                                (a, b) -> a,
                                                HashMap::new));
        enrichedUsers.put(memberId, member);

        Map<Long, List<TaskRespVO.AssignedUserVO.GroupVO>> groupsByDeptId =
                buildGroupsByDept(enrichedUsers);

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
                            Long assignerId = parseUserId(task.getCreator());
                            UserDO assigner =
                                    assignerId != null ? enrichedUsers.get(assignerId) : null;
                            return TaskRespVOBuilder.from(task)
                                    .withEvent(event)
                                    .withEventSupplier(() -> fetchEvent(task.getEventId()))
                                    .withAssignee(member)
                                    .withAssigneeSupplier(() -> fetchUserById(task.getUserId()))
                                    .withAssigner(assigner)
                                    .withAssignerSupplier(() -> fetchUserById(assignerId))
                                    .withGroupResolver(
                                            assignedUser ->
                                                    resolveGroups(
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
        dashboard.setTasks(listTasksByMember(memberId));
        return dashboard;
    }

    private EventDO fetchEvent(Long eventId) {
        if (eventId == null) {
            return null;
        }
        return eventMapper.selectById(eventId);
    }

    private Long parseUserId(String userIdValue) {
        if (!StringUtils.hasText(userIdValue)) {
            return null;
        }
        try {
            return Long.valueOf(userIdValue);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private UserDO fetchUserById(Long userId) {
        return userId != null ? userMapper.selectById(userId) : null;
    }

    private UserDO resolveAssigner(Long assignerId, UserDO preferredAssignee) {
        if (assignerId == null) {
            return null;
        }
        if (preferredAssignee != null && Objects.equals(preferredAssignee.getId(), assignerId)) {
            return preferredAssignee;
        }
        return fetchUserById(assignerId);
    }

    private Set<Long> collectRelatedUserIds(List<TaskDO> tasks) {
        Set<Long> ids = new HashSet<>();
        for (TaskDO task : tasks) {
            if (task.getUserId() != null) {
                ids.add(task.getUserId());
            }
            Long assignerId = parseUserId(task.getCreator());
            if (assignerId != null) {
                ids.add(assignerId);
            }
        }
        return ids;
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
