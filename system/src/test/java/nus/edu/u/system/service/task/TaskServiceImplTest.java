package nus.edu.u.system.service.task;

import static nus.edu.u.system.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import nus.edu.u.common.exception.ServiceException;
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
import nus.edu.u.system.enums.task.TaskStatusEnum;
import nus.edu.u.system.mapper.dept.DeptMapper;
import nus.edu.u.system.mapper.task.EventMapper;
import nus.edu.u.system.mapper.task.TaskMapper;
import nus.edu.u.system.mapper.user.UserMapper;
import nus.edu.u.system.service.task.action.TaskActionFactory;
import nus.edu.u.system.service.task.action.TaskStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock private TaskMapper taskMapper;
    @Mock private EventMapper eventMapper;
    @Mock private UserMapper userMapper;
    @Mock private DeptMapper deptMapper;
    @Mock private TaskActionFactory taskActionFactory;
    @Mock private TaskStrategy taskStrategy;

    @InjectMocks private TaskServiceImpl service;

    // ---------- Helper methods ----------

    private EventDO mockEvent(Long eventId, Long tenantId) {
        EventDO eventDO =
                EventDO.builder()
                        .id(eventId)
                        .startTime(LocalDateTime.of(2025, 11, 15, 9, 0))
                        .endTime(LocalDateTime.of(2025, 11, 15, 17, 0))
                        .build();
        eventDO.setTenantId(tenantId);
        return eventDO;
    }

    private UserDO mockUser(Long userId, Long tenantId, Long deptId) {
        UserDO userDO =
                UserDO.builder().id(userId).username("User" + userId).deptId(deptId).build();
        userDO.setTenantId(tenantId);
        return userDO;
    }

    private TaskDO mockTask(Long taskId, Long eventId, Long userId) {
        return TaskDO.builder()
                .id(taskId)
                .eventId(eventId)
                .userId(userId)
                .name("Test Task")
                .description("Test Description")
                .status(TaskStatusEnum.PROGRESS.getStatus())
                .startTime(LocalDateTime.of(2025, 11, 15, 10, 0))
                .endTime(LocalDateTime.of(2025, 11, 15, 12, 0))
                .build();
    }

    private TaskCreateReqVO mockCreateReqVO(Long targetUserId) {
        TaskCreateReqVO reqVO = new TaskCreateReqVO();
        reqVO.setName("New Task");
        reqVO.setDescription("New Task Description");
        reqVO.setTargetUserId(targetUserId);
        reqVO.setStartTime(LocalDateTime.of(2025, 11, 15, 10, 0));
        reqVO.setEndTime(LocalDateTime.of(2025, 11, 15, 12, 0));
        return reqVO;
    }

    private DeptDO mockDept(Long deptId, String name) {
        return DeptDO.builder().id(deptId).name(name).build();
    }

    // ---------- createTask tests ----------

    @Test
    void createTask_eventNotFound_throws() {
        Long eventId = 1L;
        TaskCreateReqVO reqVO = mockCreateReqVO(201L);

        when(eventMapper.selectById(eventId)).thenReturn(null);

        assertThatThrownBy(() -> service.createTask(eventId, reqVO))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(EVENT_NOT_FOUND.getCode());
    }

    @Test
    void createTask_assigneeNotFound_throws() {
        Long eventId = 1L;
        Long tenantId = 100L;
        Long userId = 201L;

        EventDO event = mockEvent(eventId, tenantId);
        TaskCreateReqVO reqVO = mockCreateReqVO(userId);

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(userMapper.selectById(userId)).thenReturn(null);

        assertThatThrownBy(() -> service.createTask(eventId, reqVO))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(TASK_ASSIGNEE_NOT_FOUND.getCode());
    }

    @Test
    void createTask_tenantMismatch_throws() {
        Long eventId = 1L;
        Long eventTenantId = 100L;
        Long userTenantId = 200L;
        Long userId = 201L;

        EventDO event = mockEvent(eventId, eventTenantId);
        UserDO assignee = mockUser(userId, userTenantId, null);
        TaskCreateReqVO reqVO = mockCreateReqVO(userId);

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(userMapper.selectById(userId)).thenReturn(assignee);

        assertThatThrownBy(() -> service.createTask(eventId, reqVO))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(TASK_ASSIGNEE_TENANT_MISMATCH.getCode());
    }

    // ---------- updateTask tests ----------

    @Test
    void updateTask_success() {
        Long eventId = 1L;
        Long taskId = 10L;
        Long tenantId = 100L;
        Long userId = 201L;
        Long organizerId = 300L;
        Integer type = TaskActionEnum.UPDATE.getCode();

        EventDO event = mockEvent(eventId, tenantId);
        event.setUserId(organizerId);
        TaskDO task = mockTask(taskId, eventId, userId);
        UserDO assignee = mockUser(userId, tenantId, 5L);
        UserDO organizer = mockUser(organizerId, tenantId, null);
        TaskUpdateReqVO reqVO = new TaskUpdateReqVO();
        reqVO.setName("Updated Task");
        reqVO.setTargetUserId(userId);

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(userMapper.selectById(userId)).thenReturn(assignee);
        when(userMapper.selectById(organizerId)).thenReturn(organizer);
        when(taskActionFactory.getStrategy(TaskActionEnum.UPDATE)).thenReturn(taskStrategy);
        when(taskStrategy.execute(any(TaskDO.class), eq(userId), any(), any())).thenReturn(true);

        TaskRespVO resp = service.updateTask(eventId, taskId, reqVO, type);

        assertThat(resp).isNotNull();
        assertThat(resp.getName()).isEqualTo("Updated Task");
        assertThat(resp.getAssignerUser()).isNotNull();
        assertThat(resp.getAssignerUser().getId()).isEqualTo(organizerId);
        verify(taskStrategy).execute(any(TaskDO.class), eq(userId), any(), any());
    }

    @Test
    void updateTask_eventNotFound_throws() {
        Long eventId = 1L;
        Long taskId = 10L;
        TaskUpdateReqVO reqVO = new TaskUpdateReqVO();
        Integer type = TaskActionEnum.UPDATE.getCode();

        when(eventMapper.selectById(eventId)).thenReturn(null);

        assertThatThrownBy(() -> service.updateTask(eventId, taskId, reqVO, type))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(EVENT_NOT_FOUND.getCode());
    }

    @Test
    void updateTask_taskNotFound_throws() {
        Long eventId = 1L;
        Long taskId = 10L;
        Long tenantId = 100L;
        TaskUpdateReqVO reqVO = new TaskUpdateReqVO();
        Integer type = TaskActionEnum.UPDATE.getCode();

        EventDO event = mockEvent(eventId, tenantId);

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(taskMapper.selectById(taskId)).thenReturn(null);

        assertThatThrownBy(() -> service.updateTask(eventId, taskId, reqVO, type))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(TASK_NOT_FOUND.getCode());
    }

    @Test
    void updateTask_taskNotBelongsToEvent_throws() {
        Long eventId = 1L;
        Long taskId = 10L;
        Long tenantId = 100L;
        Long userId = 201L;
        TaskUpdateReqVO reqVO = new TaskUpdateReqVO();
        Integer type = TaskActionEnum.UPDATE.getCode();

        EventDO event = mockEvent(eventId, tenantId);
        TaskDO task = mockTask(taskId, 999L, userId); // Different event

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(taskMapper.selectById(taskId)).thenReturn(task);

        assertThatThrownBy(() -> service.updateTask(eventId, taskId, reqVO, type))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(TASK_NOT_FOUND.getCode());
    }

    @Test
    void updateTask_assigneeNotFound_throws() {
        Long eventId = 1L;
        Long taskId = 10L;
        Long tenantId = 100L;
        Long userId = 201L;
        Long newUserId = 202L;
        Integer type = TaskActionEnum.UPDATE.getCode();

        EventDO event = mockEvent(eventId, tenantId);
        TaskDO task = mockTask(taskId, eventId, userId);
        TaskUpdateReqVO reqVO = new TaskUpdateReqVO();
        reqVO.setTargetUserId(newUserId);

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(userMapper.selectById(newUserId)).thenReturn(null);

        assertThatThrownBy(() -> service.updateTask(eventId, taskId, reqVO, type))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(TASK_ASSIGNEE_NOT_FOUND.getCode());
    }

    @Test
    void updateTask_tenantMismatch_throws() {
        Long eventId = 1L;
        Long taskId = 10L;
        Long tenantId = 100L;
        Long userId = 201L;
        Long newUserId = 202L;
        Integer type = TaskActionEnum.UPDATE.getCode();

        EventDO event = mockEvent(eventId, tenantId);
        TaskDO task = mockTask(taskId, eventId, userId);
        UserDO newAssignee = mockUser(newUserId, 200L, null); // Different tenant
        TaskUpdateReqVO reqVO = new TaskUpdateReqVO();
        reqVO.setTargetUserId(newUserId);

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(userMapper.selectById(newUserId)).thenReturn(newAssignee);

        assertThatThrownBy(() -> service.updateTask(eventId, taskId, reqVO, type))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(TASK_ASSIGNEE_TENANT_MISMATCH.getCode());
    }

    @Test
    void updateTask_wrongActionType_throws() {
        Long eventId = 1L;
        Long taskId = 10L;
        Long tenantId = 100L;
        Long userId = 201L;
        Integer type = 999; // Invalid type

        EventDO event = mockEvent(eventId, tenantId);
        TaskDO task = mockTask(taskId, eventId, userId);
        UserDO assignee = mockUser(userId, tenantId, null);
        TaskUpdateReqVO reqVO = new TaskUpdateReqVO();
        reqVO.setTargetUserId(userId);

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(userMapper.selectById(userId)).thenReturn(assignee);

        assertThatThrownBy(() -> service.updateTask(eventId, taskId, reqVO, type))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(WRONG_TASK_ACTION_TYPE.getCode());
    }

    @Test
    void updateTask_strategyExecuteFailed_throws() {
        Long eventId = 1L;
        Long taskId = 10L;
        Long tenantId = 100L;
        Long userId = 201L;
        Integer type = TaskActionEnum.UPDATE.getCode();

        EventDO event = mockEvent(eventId, tenantId);
        TaskDO task = mockTask(taskId, eventId, userId);
        UserDO assignee = mockUser(userId, tenantId, null);
        TaskUpdateReqVO reqVO = new TaskUpdateReqVO();
        reqVO.setTargetUserId(userId);

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(userMapper.selectById(userId)).thenReturn(assignee);
        when(taskActionFactory.getStrategy(TaskActionEnum.UPDATE)).thenReturn(taskStrategy);
        when(taskStrategy.execute(any(TaskDO.class), eq(userId), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.updateTask(eventId, taskId, reqVO, type))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(UPDATE_FAILURE.getCode());
    }

    @Test
    void updateTask_withoutTargetUserId_usesExistingUserId() {
        Long eventId = 1L;
        Long taskId = 10L;
        Long tenantId = 100L;
        Long existingUserId = 201L;
        Integer type = TaskActionEnum.UPDATE.getCode();

        EventDO event = mockEvent(eventId, tenantId);
        TaskDO task = mockTask(taskId, eventId, existingUserId);
        UserDO assignee = mockUser(existingUserId, tenantId, null);
        TaskUpdateReqVO reqVO = new TaskUpdateReqVO();
        reqVO.setName("Updated Task");
        // No targetUserId set

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(userMapper.selectById(existingUserId)).thenReturn(assignee);
        when(taskActionFactory.getStrategy(TaskActionEnum.UPDATE)).thenReturn(taskStrategy);
        when(taskStrategy.execute(any(TaskDO.class), eq(existingUserId), any(), any()))
                .thenReturn(true);

        TaskRespVO resp = service.updateTask(eventId, taskId, reqVO, type);

        assertThat(resp).isNotNull();
        verify(userMapper).selectById(existingUserId);
    }

    // ---------- deleteTask tests ----------

    @Test
    void deleteTask_success() {
        Long eventId = 1L;
        Long taskId = 10L;
        Long tenantId = 100L;
        Long userId = 201L;

        EventDO event = mockEvent(eventId, tenantId);
        TaskDO task = mockTask(taskId, eventId, userId);

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(taskActionFactory.getStrategy(TaskActionEnum.DELETE)).thenReturn(taskStrategy);
        when(taskStrategy.execute(eq(task), isNull())).thenReturn(true);

        assertThatCode(() -> service.deleteTask(eventId, taskId)).doesNotThrowAnyException();

        verify(taskActionFactory).getStrategy(TaskActionEnum.DELETE);
        verify(taskStrategy).execute(eq(task), isNull());
    }

    @Test
    void deleteTask_eventNotFound_throws() {
        Long eventId = 1L;
        Long taskId = 10L;

        when(eventMapper.selectById(eventId)).thenReturn(null);

        assertThatThrownBy(() -> service.deleteTask(eventId, taskId))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(EVENT_NOT_FOUND.getCode());
    }

    @Test
    void deleteTask_taskNotFound_throws() {
        Long eventId = 1L;
        Long taskId = 10L;
        Long tenantId = 100L;

        EventDO event = mockEvent(eventId, tenantId);

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(taskMapper.selectById(taskId)).thenReturn(null);

        assertThatThrownBy(() -> service.deleteTask(eventId, taskId))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(TASK_NOT_FOUND.getCode());
    }

    @Test
    void deleteTask_strategyExecuteFailed_throws() {
        Long eventId = 1L;
        Long taskId = 10L;
        Long tenantId = 100L;
        Long userId = 201L;

        EventDO event = mockEvent(eventId, tenantId);
        TaskDO task = mockTask(taskId, eventId, userId);

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(taskActionFactory.getStrategy(TaskActionEnum.DELETE)).thenReturn(taskStrategy);
        when(taskStrategy.execute(eq(task), isNull())).thenReturn(false);

        assertThatThrownBy(() -> service.deleteTask(eventId, taskId))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(TASK_DELETE_FAILED.getCode());
    }

    // ---------- getTask tests ----------

    @Test
    void getTask_success() {
        Long eventId = 1L;
        Long taskId = 10L;
        Long tenantId = 100L;
        Long userId = 201L;
        Long organizerId = 300L;

        EventDO event = mockEvent(eventId, tenantId);
        event.setUserId(organizerId);
        TaskDO task = mockTask(taskId, eventId, userId);
        UserDO user = mockUser(userId, tenantId, null);
        UserDO organizer = mockUser(organizerId, tenantId, null);

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(userMapper.selectById(userId)).thenReturn(user);
        when(userMapper.selectById(organizerId)).thenReturn(organizer);

        TaskRespVO resp = service.getTask(eventId, taskId);

        assertThat(resp).isNotNull();
        assertThat(resp.getId()).isEqualTo(taskId);
        assertThat(resp.getName()).isEqualTo("Test Task");
        assertThat(resp.getAssignerUser()).isNotNull();
        assertThat(resp.getAssignerUser().getId()).isEqualTo(organizerId);
    }

    @Test
    void getTask_eventNotFound_throws() {
        Long eventId = 1L;
        Long taskId = 10L;

        when(eventMapper.selectById(eventId)).thenReturn(null);

        assertThatThrownBy(() -> service.getTask(eventId, taskId))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(EVENT_NOT_FOUND.getCode());
    }

    @Test
    void getTask_taskNotFound_throws() {
        Long eventId = 1L;
        Long taskId = 10L;
        Long tenantId = 100L;

        EventDO event = mockEvent(eventId, tenantId);

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(taskMapper.selectById(taskId)).thenReturn(null);

        assertThatThrownBy(() -> service.getTask(eventId, taskId))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(TASK_NOT_FOUND.getCode());
    }

    // ---------- listTasksByEvent tests ----------

    @Test
    void listTasksByEvent_success() {
        Long eventId = 1L;
        Long tenantId = 100L;
        Long userId1 = 201L;
        Long userId2 = 202L;
        Long deptId1 = 5L;
        Long deptId2 = 6L;
        Long organizerId = 300L;

        EventDO event = mockEvent(eventId, tenantId);
        event.setUserId(organizerId);
        TaskDO task1 = mockTask(1L, eventId, userId1);
        TaskDO task2 = mockTask(2L, eventId, userId2);
        UserDO user1 = mockUser(userId1, tenantId, deptId1);
        UserDO user2 = mockUser(userId2, tenantId, deptId2);
        UserDO organizer = mockUser(organizerId, tenantId, null);
        DeptDO dept1 = mockDept(deptId1, "Dept A");
        DeptDO dept2 = mockDept(deptId2, "Dept B");

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(taskMapper.selectList(any())).thenReturn(List.of(task1, task2));
        when(userMapper.selectBatchIds(List.of(userId1, userId2)))
                .thenReturn(List.of(user1, user2));
        when(userMapper.selectById(organizerId)).thenReturn(organizer);
        when(deptMapper.selectBatchIds(List.of(deptId1, deptId2)))
                .thenReturn(List.of(dept1, dept2));

        List<TaskRespVO> resp = service.listTasksByEvent(eventId);

        assertThat(resp).hasSize(2);
        assertThat(resp).extracting(TaskRespVO::getId).containsExactly(1L, 2L);
        assertThat(resp.get(0).getAssignedUser()).isNotNull();
        assertThat(resp.get(0).getAssignedUser().getGroups()).hasSize(1);
        assertThat(resp.get(0).getAssignedUser().getGroups().get(0).getName()).isEqualTo("Dept A");
        assertThat(resp.get(0).getAssignerUser()).isNotNull();
        assertThat(resp.get(0).getAssignerUser().getId()).isEqualTo(organizerId);
    }

    @Test
    void listTasksByEvent_eventNotFound_throws() {
        Long eventId = 1L;

        when(eventMapper.selectById(eventId)).thenReturn(null);

        assertThatThrownBy(() -> service.listTasksByEvent(eventId))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(EVENT_NOT_FOUND.getCode());
    }

    @Test
    void listTasksByEvent_noTasks_returnsEmptyList() {
        Long eventId = 1L;
        Long tenantId = 100L;

        EventDO event = mockEvent(eventId, tenantId);

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(taskMapper.selectList(any())).thenReturn(List.of());

        List<TaskRespVO> resp = service.listTasksByEvent(eventId);

        assertThat(resp).isEmpty();
    }

    @Test
    void listTasksByEvent_tasksWithoutUsers_success() {
        Long eventId = 1L;
        Long tenantId = 100L;
        Long organizerId = 300L;

        EventDO event = mockEvent(eventId, tenantId);
        event.setUserId(organizerId);
        TaskDO task1 = mockTask(1L, eventId, null); // No user assigned
        task1.setUserId(null);
        UserDO organizer = mockUser(organizerId, tenantId, null);

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(taskMapper.selectList(any())).thenReturn(List.of(task1));
        when(userMapper.selectById(organizerId)).thenReturn(organizer);

        List<TaskRespVO> resp = service.listTasksByEvent(eventId);

        assertThat(resp).hasSize(1);
        verify(userMapper, never()).selectBatchIds(any());
    }

    // ---------- Private method tests ----------

    @Test
    void buildCrudGroupsByDept_emptyUsersMap_returnsEmptyMap() {
        Map<Long, List<TaskRespVO.AssignedUserVO.GroupVO>> result =
                ReflectionTestUtils.invokeMethod(service, "buildCrudGroupsByDept", Map.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(deptMapper);
    }

    @Test
    void buildCrudGroupsByDept_usersWithoutDept_returnsEmptyMap() {
        UserDO user1 = mockUser(1L, 100L, null);
        Map<Long, UserDO> usersMap = Map.of(1L, user1);

        Map<Long, List<TaskRespVO.AssignedUserVO.GroupVO>> result =
                ReflectionTestUtils.invokeMethod(service, "buildCrudGroupsByDept", usersMap);

        assertThat(result).isEmpty();
        verifyNoInteractions(deptMapper);
    }

    @Test
    void buildCrudGroupsByDept_usersWithDept_returnsGroupMap() {
        UserDO user1 = mockUser(1L, 100L, 5L);
        UserDO user2 = mockUser(2L, 100L, 6L);
        Map<Long, UserDO> usersMap = Map.of(1L, user1, 2L, user2);

        DeptDO dept1 = mockDept(5L, "Dept A");
        DeptDO dept2 = mockDept(6L, "Dept B");

        // 使用 anyList() 而不是具体的 List.of(5L, 6L)
        when(deptMapper.selectBatchIds(anyList())).thenReturn(List.of(dept1, dept2));

        Map<Long, List<TaskRespVO.AssignedUserVO.GroupVO>> result =
                ReflectionTestUtils.invokeMethod(service, "buildCrudGroupsByDept", usersMap);

        assertThat(result).hasSize(2);
        assertThat(result.get(5L)).hasSize(1);
        assertThat(result.get(5L).get(0).getName()).isEqualTo("Dept A");
        assertThat(result.get(6L)).hasSize(1);
        assertThat(result.get(6L).get(0).getName()).isEqualTo("Dept B");
    }

    @Test
    void resolveCrudGroups_deptIdNull_returnsEmptyList() {
        List<TaskRespVO.AssignedUserVO.GroupVO> result =
                ReflectionTestUtils.invokeMethod(service, "resolveCrudGroups", null, null);

        assertThat(result).isEmpty();
    }

    @Test
    void resolveCrudGroups_deptInCache_returnsCachedGroups() {
        Long deptId = 5L;
        TaskRespVO.AssignedUserVO.GroupVO groupVO = new TaskRespVO.AssignedUserVO.GroupVO();
        groupVO.setId(deptId);
        groupVO.setName("Cached Dept");

        Map<Long, List<TaskRespVO.AssignedUserVO.GroupVO>> cache = Map.of(deptId, List.of(groupVO));

        List<TaskRespVO.AssignedUserVO.GroupVO> result =
                ReflectionTestUtils.invokeMethod(service, "resolveCrudGroups", deptId, cache);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Cached Dept");
        verifyNoInteractions(deptMapper);
    }

    @Test
    void resolveCrudGroups_deptNotInCache_fetchesFromDb() {
        Long deptId = 5L;
        DeptDO dept = mockDept(deptId, "DB Dept");

        when(deptMapper.selectById(deptId)).thenReturn(dept);

        List<TaskRespVO.AssignedUserVO.GroupVO> result =
                ReflectionTestUtils.invokeMethod(service, "resolveCrudGroups", deptId, Map.of());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("DB Dept");
        verify(deptMapper).selectById(deptId);
    }

    @Test
    void resolveCrudGroups_deptNotFound_returnsEmptyList() {
        Long deptId = 5L;

        when(deptMapper.selectById(deptId)).thenReturn(null);

        List<TaskRespVO.AssignedUserVO.GroupVO> result =
                ReflectionTestUtils.invokeMethod(service, "resolveCrudGroups", deptId, Map.of());

        assertThat(result).isEmpty();
    }

    @Test
    void toCrudGroupVO_convertsCorrectly() {
        DeptDO dept = mockDept(5L, "Test Dept");

        TaskRespVO.AssignedUserVO.GroupVO result =
                ReflectionTestUtils.invokeMethod(service, "toCrudGroupVO", dept);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getName()).isEqualTo("Test Dept");
    }

    @Test
    void resolveAssignerGroups_fetchesDept() {
        Long deptId = 5L;
        DeptDO dept = mockDept(deptId, "Assigner Dept");

        when(deptMapper.selectById(deptId)).thenReturn(dept);

        List<TaskRespVO.AssignerUserVO.GroupVO> result =
                ReflectionTestUtils.invokeMethod(service, "resolveAssignerGroups", deptId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Assigner Dept");
    }

    @Test
    void buildDashboardGroupsByDept_usersWithDept_returnsGroupMap() {
        UserDO user1 = mockUser(1L, 100L, 5L);
        UserDO user2 = mockUser(2L, 100L, 6L);
        Map<Long, UserDO> usersMap = Map.of(1L, user1, 2L, user2);

        DeptDO dept1 = mockDept(5L, "Dept A");
        dept1.setEventId(50L);
        DeptDO dept2 = mockDept(6L, "Dept B");
        dept2.setEventId(60L);

        when(deptMapper.selectBatchIds(anyList())).thenReturn(List.of(dept1, dept2));

        Map<Long, List<TasksRespVO.AssignedUserVO.GroupVO>> result =
                ReflectionTestUtils.invokeMethod(service, "buildDashboardGroupsByDept", usersMap);

        assertThat(result).hasSize(2);
        assertThat(result.get(5L).get(0).getEventId()).isEqualTo(50L);
        assertThat(result.get(6L).get(0).getName()).isEqualTo("Dept B");
    }

    @Test
    void resolveDashboardGroups_deptInCache_returnsCachedGroups() {
        Long deptId = 5L;
        TasksRespVO.AssignedUserVO.GroupVO cached = new TasksRespVO.AssignedUserVO.GroupVO();
        cached.setId(deptId);
        cached.setName("Cached Dept");

        Map<Long, List<TasksRespVO.AssignedUserVO.GroupVO>> cache = Map.of(deptId, List.of(cached));

        List<TasksRespVO.AssignedUserVO.GroupVO> result =
                ReflectionTestUtils.invokeMethod(service, "resolveDashboardGroups", deptId, cache);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Cached Dept");
    }

    @Test
    void resolveDashboardGroups_deptNotFound_returnsEmptyList() {
        Long deptId = 5L;
        when(deptMapper.selectById(deptId)).thenReturn(null);

        List<TasksRespVO.AssignedUserVO.GroupVO> result =
                ReflectionTestUtils.invokeMethod(service, "resolveDashboardGroups", deptId, Map.of());

        assertThat(result).isEmpty();
    }

    @Test
    void toDashboardGroupVO_convertsCorrectly() {
        DeptDO dept = mockDept(5L, "Dashboard Dept");
        dept.setEventId(77L);
        dept.setLeadUserId(88L);
        dept.setRemark("dashboard");

        TasksRespVO.AssignedUserVO.GroupVO result =
                ReflectionTestUtils.invokeMethod(service, "toDashboardGroupVO", dept);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getEventId()).isEqualTo(77L);
        assertThat(result.getLeadUserId()).isEqualTo(88L);
        assertThat(result.getRemark()).isEqualTo("dashboard");
    }

    @Test
    void buildAssignersByEventId_returnsOrganizerMap() {
        EventDO event1 = mockEvent(1L, 100L);
        event1.setUserId(200L);
        EventDO event2 = mockEvent(2L, 100L);
        event2.setUserId(201L);

        UserDO user1 = mockUser(200L, 100L, null);
        UserDO user2 = mockUser(201L, 100L, null);

        when(userMapper.selectBatchIds(List.of(200L, 201L))).thenReturn(List.of(user1, user2));

        Map<Long, UserDO> result =
                ReflectionTestUtils.invokeMethod(
                        service, "buildAssignersByEventId", List.of(event1, event2));

        assertThat(result).hasSize(2);
        assertThat(result.get(1L)).isEqualTo(user1);
        assertThat(result.get(2L)).isEqualTo(user2);
    }

    // ---------- listTasksByMember tests ----------

    @Test
    void listTasksByMember_success() {
        Long memberId = 201L;
        Long tenantId = 100L;
        Long deptId = 5L;
        Long eventId1 = 1L;
        Long eventId2 = 2L;
        Long organizerId1 = 301L;
        Long organizerId2 = 302L;

        UserDO member = mockUser(memberId, tenantId, deptId);
        TaskDO task1 = mockTask(10L, eventId1, memberId);
        TaskDO task2 = mockTask(11L, eventId2, memberId);
        EventDO event1 = mockEvent(eventId1, tenantId);
        EventDO event2 = mockEvent(eventId2, tenantId);
        event1.setUserId(organizerId1);
        event2.setUserId(organizerId2);
        DeptDO dept = mockDept(deptId, "Dept A");
        UserDO organizer1 = mockUser(organizerId1, tenantId, null);
        UserDO organizer2 = mockUser(organizerId2, tenantId, null);

        when(userMapper.selectById(memberId)).thenReturn(member);
        when(taskMapper.selectList(any())).thenReturn(List.of(task1, task2));
        when(eventMapper.selectBatchIds(List.of(eventId1, eventId2)))
                .thenReturn(List.of(event1, event2));
        when(userMapper.selectBatchIds(List.of(organizerId1, organizerId2)))
                .thenReturn(List.of(organizer1, organizer2));
        when(deptMapper.selectBatchIds(List.of(deptId))).thenReturn(List.of(dept));

        List<TaskRespVO> resp = service.listTasksByMember(memberId);

        assertThat(resp).hasSize(2);
        assertThat(resp).extracting(TaskRespVO::getId).containsExactly(10L, 11L);
        assertThat(resp.get(0).getAssignedUser()).isNotNull();
        assertThat(resp.get(0).getAssignedUser().getId()).isEqualTo(memberId);
        assertThat(resp.get(0).getEvent()).isNotNull();
        assertThat(resp.get(0).getAssignerUser()).isNotNull();
        assertThat(resp.get(0).getAssignerUser().getId()).isEqualTo(organizerId1);
    }

    @Test
    void listTasksByMember_memberNotFound_throws() {
        Long memberId = 201L;

        when(userMapper.selectById(memberId)).thenReturn(null);

        assertThatThrownBy(() -> service.listTasksByMember(memberId))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(USER_NOT_FOUND.getCode());
    }

    @Test
    void listTasksByMember_noTasks_returnsEmptyList() {
        Long memberId = 201L;
        Long tenantId = 100L;

        UserDO member = mockUser(memberId, tenantId, null);

        when(userMapper.selectById(memberId)).thenReturn(member);
        when(taskMapper.selectList(any())).thenReturn(List.of());

        List<TaskRespVO> resp = service.listTasksByMember(memberId);

        assertThat(resp).isEmpty();
    }

    @Test
    void listTasksByMember_withNullEventId_handlesGracefully() {
        Long memberId = 201L;
        Long tenantId = 100L;

        UserDO member = mockUser(memberId, tenantId, null);
        TaskDO task = mockTask(10L, null, memberId); // null eventId
        task.setEventId(null);

        when(userMapper.selectById(memberId)).thenReturn(member);
        when(taskMapper.selectList(any())).thenReturn(List.of(task));

        List<TaskRespVO> resp = service.listTasksByMember(memberId);

        assertThat(resp).hasSize(1);
        assertThat(resp.get(0).getEvent()).isNull();
    }

    // ---------- getByMemberId tests ----------

    @Test
    void getByMemberId_success() {
        Long memberId = 201L;
        Long tenantId = 100L;
        Long deptId = 5L;
        Long eventId = 1L;
        Long organizerId = 300L;

        UserDO member = mockUser(memberId, tenantId, deptId);
        TaskDO task = mockTask(10L, eventId, memberId);
        EventDO event = mockEvent(eventId, tenantId);
        event.setUserId(organizerId);
        DeptDO dept = mockDept(deptId, "Dept A");
        dept.setEventId(eventId);

        when(userMapper.selectById(memberId)).thenReturn(member);
        when(taskMapper.selectList(any())).thenReturn(List.of(task));
        when(eventMapper.selectBatchIds(List.of(eventId))).thenReturn(List.of(event));
        when(deptMapper.selectBatchIds(List.of(deptId))).thenReturn(List.of(dept));
        when(deptMapper.selectById(deptId)).thenReturn(dept);
        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(userMapper.selectById(memberId)).thenReturn(member);

        TaskDashboardRespVO resp = service.getByMemberId(memberId);

        assertThat(resp).isNotNull();
        assertThat(resp.getMember()).isNotNull();
        assertThat(resp.getMember().getId()).isEqualTo(memberId);
        assertThat(resp.getMember().getUsername()).isEqualTo("User" + memberId);
        assertThat(resp.getGroups()).hasSize(1);
        assertThat(resp.getGroups().get(0).getName()).isEqualTo("Dept A");
        assertThat(resp.getTasks()).hasSize(1);
        TasksRespVO dashboardTask = resp.getTasks().get(0);
        assertThat(dashboardTask.getAssignedUser()).isNotNull();
        assertThat(dashboardTask.getAssignedUser().getId()).isEqualTo(memberId);
    }

    @Test
    void getByMemberId_memberNotFound_throws() {
        Long memberId = 201L;

        when(userMapper.selectById(memberId)).thenReturn(null);

        assertThatThrownBy(() -> service.getByMemberId(memberId))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(USER_NOT_FOUND.getCode());
    }

    @Test
    void getByMemberId_memberWithoutDept_success() {
        Long memberId = 201L;
        Long tenantId = 100L;

        UserDO member = mockUser(memberId, tenantId, null); // No department

        when(userMapper.selectById(memberId)).thenReturn(member);
        when(taskMapper.selectList(any())).thenReturn(List.of());

        TaskDashboardRespVO resp = service.getByMemberId(memberId);

        assertThat(resp).isNotNull();
        assertThat(resp.getMember()).isNotNull();
        assertThat(resp.getGroups()).isEmpty();
        assertThat(resp.getTasks()).isEmpty();
    }

    @Test
    void getByMemberId_deptNotFound_returnsEmptyGroups() {
        Long memberId = 201L;
        Long tenantId = 100L;
        Long deptId = 5L;

        UserDO member = mockUser(memberId, tenantId, deptId);

        when(userMapper.selectById(memberId)).thenReturn(member);
        when(taskMapper.selectList(any())).thenReturn(List.of());
        when(deptMapper.selectById(deptId)).thenReturn(null);

        TaskDashboardRespVO resp = service.getByMemberId(memberId);

        assertThat(resp).isNotNull();
        assertThat(resp.getGroups()).isEmpty();
    }

    // ---------- Private method tests ----------

    @Test
    void toMemberVO_convertsCorrectly() {
        Long memberId = 201L;
        Long tenantId = 100L;
        UserDO member = mockUser(memberId, tenantId, 5L);
        member.setEmail("test@example.com");
        member.setPhone("1234567890");
        member.setStatus(1);
        member.setCreateTime(LocalDateTime.of(2025, 1, 1, 10, 0));
        member.setUpdateTime(LocalDateTime.of(2025, 1, 2, 10, 0));

        TaskDashboardRespVO.MemberVO result =
                ReflectionTestUtils.invokeMethod(service, "toMemberVO", member);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(memberId);
        assertThat(result.getUsername()).isEqualTo("User" + memberId);
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getPhone()).isEqualTo("1234567890");
        assertThat(result.getStatus()).isEqualTo(1);
        assertThat(result.getCreateTime()).isEqualTo(LocalDateTime.of(2025, 1, 1, 10, 0));
        assertThat(result.getUpdateTime()).isEqualTo(LocalDateTime.of(2025, 1, 2, 10, 0));
    }

    @Test
    void resolveMemberGroups_withDept_returnsGroups() {
        Long memberId = 201L;
        Long tenantId = 100L;
        Long deptId = 5L;
        Long eventId = 1L;

        UserDO member = mockUser(memberId, tenantId, deptId);
        DeptDO dept = mockDept(deptId, "Dept A");
        dept.setEventId(eventId);
        dept.setLeadUserId(100L);
        dept.setRemark("Test Remark");
        dept.setStatus(1);
        dept.setSort(1);

        EventDO event = mockEvent(eventId, tenantId);

        when(deptMapper.selectById(deptId)).thenReturn(dept);
        when(eventMapper.selectById(eventId)).thenReturn(event);

        List<TaskDashboardRespVO.GroupVO> result =
                ReflectionTestUtils.invokeMethod(service, "resolveMemberGroups", member);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(deptId);
        assertThat(result.get(0).getName()).isEqualTo("Dept A");
        assertThat(result.get(0).getLeadUserId()).isEqualTo(100L);
        assertThat(result.get(0).getEvent()).isNotNull();
    }

    @Test
    void resolveMemberGroups_noDept_returnsEmptyList() {
        Long memberId = 201L;
        Long tenantId = 100L;

        UserDO member = mockUser(memberId, tenantId, null); // No department

        List<TaskDashboardRespVO.GroupVO> result =
                ReflectionTestUtils.invokeMethod(service, "resolveMemberGroups", member);

        assertThat(result).isEmpty();
        verifyNoInteractions(deptMapper);
    }

    @Test
    void resolveMemberGroups_deptNotFound_returnsEmptyList() {
        Long memberId = 201L;
        Long tenantId = 100L;
        Long deptId = 5L;

        UserDO member = mockUser(memberId, tenantId, deptId);

        when(deptMapper.selectById(deptId)).thenReturn(null);

        List<TaskDashboardRespVO.GroupVO> result =
                ReflectionTestUtils.invokeMethod(service, "resolveMemberGroups", member);

        assertThat(result).isEmpty();
    }

    @Test
    void toGroupEvent_validEventId_convertsCorrectly() {
        Long eventId = 1L;
        Long tenantId = 100L;

        EventDO event = mockEvent(eventId, tenantId);
        event.setName("Test Event");
        event.setDescription("Test Description");
        event.setLocation("Test Location");
        event.setStatus(1);
        event.setRemark("Test Remark");

        when(eventMapper.selectById(eventId)).thenReturn(event);

        TaskDashboardRespVO.GroupVO.EventVO result =
                ReflectionTestUtils.invokeMethod(service, "toGroupEvent", eventId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(eventId);
        assertThat(result.getName()).isEqualTo("Test Event");
        assertThat(result.getDescription()).isEqualTo("Test Description");
        assertThat(result.getLocation()).isEqualTo("Test Location");
        assertThat(result.getStatus()).isEqualTo(1);
        assertThat(result.getRemark()).isEqualTo("Test Remark");
    }

    @Test
    void toGroupEvent_nullEventId_returnsNull() {
        TaskDashboardRespVO.GroupVO.EventVO result =
                ReflectionTestUtils.invokeMethod(service, "toGroupEvent", new Object[] {null});

        assertThat(result).isNull();
        verifyNoInteractions(eventMapper);
    }

    @Test
    void toGroupEvent_eventNotFound_returnsNull() {
        Long eventId = 1L;

        when(eventMapper.selectById(eventId)).thenReturn(null);

        TaskDashboardRespVO.GroupVO.EventVO result =
                ReflectionTestUtils.invokeMethod(service, "toGroupEvent", eventId);

        assertThat(result).isNull();
    }
}
