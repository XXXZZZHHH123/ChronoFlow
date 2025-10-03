package nus.edu.u.system.service.task;

import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.*;
import static nus.edu.u.system.enums.task.TaskActionEnum.getUpdateTaskAction;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
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
import nus.edu.u.system.enums.task.TaskActionEnum;
import nus.edu.u.system.mapper.dept.DeptMapper;
import nus.edu.u.system.mapper.task.EventMapper;
import nus.edu.u.system.mapper.task.TaskMapper;
import nus.edu.u.system.mapper.user.UserMapper;
import nus.edu.u.system.service.task.action.TaskActionFactory;
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

        return buildTaskResponse(task, assignee);
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
                        task -> {
                            Long userId = task.getUserId();
                            UserDO user = userId != null ? usersById.get(userId) : null;
                            return buildTaskResponse(task, user, groupsByDeptId);
                        })
                .toList();
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
