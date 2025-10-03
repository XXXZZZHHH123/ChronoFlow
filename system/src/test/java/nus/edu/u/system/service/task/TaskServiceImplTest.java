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
import nus.edu.u.system.domain.vo.task.TaskRespVO;
import nus.edu.u.system.domain.vo.task.TaskUpdateReqVO;
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
        Integer type = TaskActionEnum.UPDATE.getCode();

        EventDO event = mockEvent(eventId, tenantId);
        TaskDO task = mockTask(taskId, eventId, userId);
        UserDO assignee = mockUser(userId, tenantId, 5L);
        TaskUpdateReqVO reqVO = new TaskUpdateReqVO();
        reqVO.setName("Updated Task");
        reqVO.setTargetUserId(userId);

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(userMapper.selectById(userId)).thenReturn(assignee);
        when(taskActionFactory.getStrategy(TaskActionEnum.UPDATE)).thenReturn(taskStrategy);
        when(taskStrategy.execute(any(TaskDO.class), eq(userId), any(), any())).thenReturn(true);

        TaskRespVO resp = service.updateTask(eventId, taskId, reqVO, type);

        assertThat(resp).isNotNull();
        assertThat(resp.getName()).isEqualTo("Updated Task");
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

        EventDO event = mockEvent(eventId, tenantId);
        TaskDO task = mockTask(taskId, eventId, userId);
        UserDO user = mockUser(userId, tenantId, null);

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(userMapper.selectById(userId)).thenReturn(user);

        TaskRespVO resp = service.getTask(eventId, taskId);

        assertThat(resp).isNotNull();
        assertThat(resp.getId()).isEqualTo(taskId);
        assertThat(resp.getName()).isEqualTo("Test Task");
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

        EventDO event = mockEvent(eventId, tenantId);
        TaskDO task1 = mockTask(1L, eventId, userId1);
        TaskDO task2 = mockTask(2L, eventId, userId2);
        UserDO user1 = mockUser(userId1, tenantId, deptId1);
        UserDO user2 = mockUser(userId2, tenantId, deptId2);
        DeptDO dept1 = mockDept(deptId1, "Dept A");
        DeptDO dept2 = mockDept(deptId2, "Dept B");

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(taskMapper.selectList(any())).thenReturn(List.of(task1, task2));
        when(userMapper.selectBatchIds(List.of(userId1, userId2)))
                .thenReturn(List.of(user1, user2));
        when(deptMapper.selectBatchIds(List.of(deptId1, deptId2)))
                .thenReturn(List.of(dept1, dept2));

        List<TaskRespVO> resp = service.listTasksByEvent(eventId);

        assertThat(resp).hasSize(2);
        assertThat(resp).extracting(TaskRespVO::getId).containsExactly(1L, 2L);
        assertThat(resp.get(0).getAssignedUser()).isNotNull();
        assertThat(resp.get(0).getAssignedUser().getGroups()).hasSize(1);
        assertThat(resp.get(0).getAssignedUser().getGroups().get(0).getName()).isEqualTo("Dept A");
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

        EventDO event = mockEvent(eventId, tenantId);
        TaskDO task1 = mockTask(1L, eventId, null); // No user assigned
        task1.setUserId(null);

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(taskMapper.selectList(any())).thenReturn(List.of(task1));

        List<TaskRespVO> resp = service.listTasksByEvent(eventId);

        assertThat(resp).hasSize(1);
        verify(userMapper, never()).selectBatchIds(any());
    }

    // ---------- Private method tests ----------

    @Test
    void buildGroupsByDept_emptyUsersMap_returnsEmptyMap() {
        Map<Long, List<TaskRespVO.AssignedUserVO.GroupVO>> result =
                ReflectionTestUtils.invokeMethod(service, "buildGroupsByDept", Map.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(deptMapper);
    }

    @Test
    void buildGroupsByDept_usersWithoutDept_returnsEmptyMap() {
        UserDO user1 = mockUser(1L, 100L, null);
        Map<Long, UserDO> usersMap = Map.of(1L, user1);

        Map<Long, List<TaskRespVO.AssignedUserVO.GroupVO>> result =
                ReflectionTestUtils.invokeMethod(service, "buildGroupsByDept", usersMap);

        assertThat(result).isEmpty();
        verifyNoInteractions(deptMapper);
    }

    @Test
    void buildGroupsByDept_usersWithDept_returnsGroupMap() {
        UserDO user1 = mockUser(1L, 100L, 5L);
        UserDO user2 = mockUser(2L, 100L, 6L);
        Map<Long, UserDO> usersMap = Map.of(1L, user1, 2L, user2);

        DeptDO dept1 = mockDept(5L, "Dept A");
        DeptDO dept2 = mockDept(6L, "Dept B");

        when(deptMapper.selectBatchIds(List.of(5L, 6L))).thenReturn(List.of(dept1, dept2));

        Map<Long, List<TaskRespVO.AssignedUserVO.GroupVO>> result =
                ReflectionTestUtils.invokeMethod(service, "buildGroupsByDept", usersMap);

        assertThat(result).hasSize(2);
        assertThat(result.get(5L)).hasSize(1);
        assertThat(result.get(5L).get(0).getName()).isEqualTo("Dept A");
        assertThat(result.get(6L)).hasSize(1);
        assertThat(result.get(6L).get(0).getName()).isEqualTo("Dept B");
    }

    @Test
    void resolveGroups_deptIdNull_returnsEmptyList() {
        List<TaskRespVO.AssignedUserVO.GroupVO> result =
                ReflectionTestUtils.invokeMethod(service, "resolveGroups", null, null);

        assertThat(result).isEmpty();
    }

    @Test
    void resolveGroups_deptInCache_returnsCachedGroups() {
        Long deptId = 5L;
        TaskRespVO.AssignedUserVO.GroupVO groupVO = new TaskRespVO.AssignedUserVO.GroupVO();
        groupVO.setId(deptId);
        groupVO.setName("Cached Dept");

        Map<Long, List<TaskRespVO.AssignedUserVO.GroupVO>> cache = Map.of(deptId, List.of(groupVO));

        List<TaskRespVO.AssignedUserVO.GroupVO> result =
                ReflectionTestUtils.invokeMethod(service, "resolveGroups", deptId, cache);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Cached Dept");
        verifyNoInteractions(deptMapper);
    }

    @Test
    void resolveGroups_deptNotInCache_fetchesFromDb() {
        Long deptId = 5L;
        DeptDO dept = mockDept(deptId, "DB Dept");

        when(deptMapper.selectById(deptId)).thenReturn(dept);

        List<TaskRespVO.AssignedUserVO.GroupVO> result =
                ReflectionTestUtils.invokeMethod(service, "resolveGroups", deptId, Map.of());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("DB Dept");
        verify(deptMapper).selectById(deptId);
    }

    @Test
    void resolveGroups_deptNotFound_returnsEmptyList() {
        Long deptId = 5L;

        when(deptMapper.selectById(deptId)).thenReturn(null);

        List<TaskRespVO.AssignedUserVO.GroupVO> result =
                ReflectionTestUtils.invokeMethod(service, "resolveGroups", deptId, Map.of());

        assertThat(result).isEmpty();
    }

    @Test
    void toGroupVO_convertsCorrectly() {
        DeptDO dept = mockDept(5L, "Test Dept");

        TaskRespVO.AssignedUserVO.GroupVO result =
                ReflectionTestUtils.invokeMethod(service, "toGroupVO", dept);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getName()).isEqualTo("Test Dept");
    }
}
