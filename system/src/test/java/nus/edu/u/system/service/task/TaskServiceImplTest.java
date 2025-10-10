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
import nus.edu.u.system.domain.dataobject.user.UserGroupDO;
import nus.edu.u.system.domain.dto.TaskActionDTO;
import nus.edu.u.system.domain.vo.task.TaskCreateReqVO;
import nus.edu.u.system.domain.vo.task.TaskDashboardRespVO;
import nus.edu.u.system.domain.vo.task.TaskRespVO;
import nus.edu.u.system.domain.vo.task.TaskUpdateReqVO;
import nus.edu.u.system.enums.task.TaskActionEnum;
import nus.edu.u.system.enums.task.TaskStatusEnum;
import nus.edu.u.system.mapper.dept.DeptMapper;
import nus.edu.u.system.mapper.task.EventMapper;
import nus.edu.u.system.mapper.task.TaskMapper;
import nus.edu.u.system.mapper.user.UserGroupMapper;
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
    @Mock private UserGroupMapper userGroupMapper;
    @Mock private DeptMapper deptMapper;
    @Mock private TaskActionFactory taskActionFactory;
    @Mock private TaskStrategy taskStrategy;

    @InjectMocks private TaskServiceImpl service;

    // ---------- Helper methods ----------

    private EventDO mockEvent(Long eventId, Long tenantId, Long assignerId) {
        EventDO eventDO =
                EventDO.builder()
                        .id(eventId)
                        .userId(assignerId)
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

    private UserGroupDO mockUserGroup(
            Long userId, Long deptId, Long eventId, LocalDateTime joinTime) {
        return UserGroupDO.builder()
                .userId(userId)
                .deptId(deptId)
                .eventId(eventId)
                .joinTime(joinTime)
                .build();
    }

    // ---------- createTask tests ----------

    @Test
    void createTask_success() {
        Long eventId = 1L;
        Long tenantId = 100L;
        Long assigneeId = 201L;
        Long assignerId = 301L;
        Long assigneeDeptId = 5L;
        Long assignerDeptId = 6L;

        EventDO event = mockEvent(eventId, tenantId, assignerId);
        UserDO assignee = mockUser(assigneeId, tenantId, assigneeDeptId);
        UserDO assigner = mockUser(assignerId, tenantId, assignerDeptId);
        TaskCreateReqVO reqVO = mockCreateReqVO(assigneeId);
        DeptDO assigneeDept = mockDept(assigneeDeptId, "Assignee Dept");
        DeptDO assignerDept = mockDept(assignerDeptId, "Assigner Dept");

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(userMapper.selectById(assigneeId)).thenReturn(assignee);
        when(userMapper.selectById(assignerId)).thenReturn(assigner);
        when(taskActionFactory.getStrategy(TaskActionEnum.CREATE)).thenReturn(taskStrategy);
        doNothing().when(taskStrategy).execute(any(TaskDO.class), any(TaskActionDTO.class));
        when(deptMapper.selectById(assigneeDeptId)).thenReturn(assigneeDept);
        when(deptMapper.selectById(assignerDeptId)).thenReturn(assignerDept);

        TaskRespVO resp = service.createTask(eventId, reqVO);

        assertThat(resp).isNotNull();
        assertThat(resp.getEvent()).isNotNull();
        assertThat(resp.getEvent().getId()).isEqualTo(eventId);

        verify(eventMapper).selectById(eventId);
        verify(userMapper).selectById(assigneeId);
        verify(userMapper).selectById(assignerId);
        verify(taskActionFactory).getStrategy(TaskActionEnum.CREATE);
        verify(taskStrategy).execute(any(TaskDO.class), any(TaskActionDTO.class));
    }

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

        EventDO event = mockEvent(eventId, tenantId, 301L);
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

        EventDO event = mockEvent(eventId, eventTenantId, 301L);
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
        Long assigneeId = 201L;
        Long assignerId = 301L;
        Long assigneeDeptId = 5L;
        Long assignerDeptId = 6L;
        Integer type = TaskActionEnum.UPDATE.getCode();

        EventDO event = mockEvent(eventId, tenantId, assignerId);
        TaskDO task = mockTask(taskId, eventId, assigneeId);
        UserDO assignee = mockUser(assigneeId, tenantId, assigneeDeptId);
        UserDO assigner = mockUser(assignerId, tenantId, assignerDeptId);
        DeptDO assigneeDept = mockDept(assigneeDeptId, "Assignee Dept");
        DeptDO assignerDept = mockDept(assignerDeptId, "Assigner Dept");
        TaskUpdateReqVO reqVO = new TaskUpdateReqVO();
        reqVO.setName("Updated Task");
        reqVO.setTargetUserId(assigneeId);

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(userMapper.selectById(assigneeId)).thenReturn(assignee);
        when(userMapper.selectById(assignerId)).thenReturn(assigner);
        when(taskActionFactory.getStrategy(TaskActionEnum.UPDATE)).thenReturn(taskStrategy);
        doNothing().when(taskStrategy).execute(any(TaskDO.class), any(TaskActionDTO.class));
        when(deptMapper.selectById(assigneeDeptId)).thenReturn(assigneeDept);
        when(deptMapper.selectById(assignerDeptId)).thenReturn(assignerDept);

        TaskRespVO resp = service.updateTask(eventId, taskId, reqVO, type);

        assertThat(resp).isNotNull();
        assertThat(resp.getName()).isEqualTo("Test Task");
        assertThat(resp.getAssignerUser()).isNotNull();
        assertThat(resp.getAssignerUser().getId()).isEqualTo(assignerId);
        verify(taskStrategy).execute(any(TaskDO.class), any(TaskActionDTO.class));
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

        EventDO event = mockEvent(eventId, tenantId, 301L);

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

        EventDO event = mockEvent(eventId, tenantId, 301L);
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

        EventDO event = mockEvent(eventId, tenantId, 301L);
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

        EventDO event = mockEvent(eventId, tenantId, 301L);
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

        EventDO event = mockEvent(eventId, tenantId, 301L);
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
    void updateTask_withoutTargetUserId_success() {
        Long eventId = 1L;
        Long taskId = 10L;
        Long tenantId = 100L;
        Long existingUserId = 201L;
        Long assignerId = 301L;
        Long deptId = 5L;
        Integer type = TaskActionEnum.UPDATE.getCode();

        EventDO event = mockEvent(eventId, tenantId, assignerId);
        TaskDO task = mockTask(taskId, eventId, existingUserId);
        UserDO assigner = mockUser(assignerId, tenantId, deptId);
        DeptDO dept = mockDept(deptId, "Test Dept");
        TaskUpdateReqVO reqVO = new TaskUpdateReqVO();
        reqVO.setName("Updated Task");

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(userMapper.selectById(assignerId)).thenReturn(assigner);
        when(taskActionFactory.getStrategy(TaskActionEnum.UPDATE)).thenReturn(taskStrategy);
        doNothing().when(taskStrategy).execute(any(TaskDO.class), any(TaskActionDTO.class));
        when(deptMapper.selectById(deptId)).thenReturn(dept);

        TaskRespVO resp = service.updateTask(eventId, taskId, reqVO, type);

        assertThat(resp).isNotNull();
        verify(taskStrategy).execute(any(TaskDO.class), any(TaskActionDTO.class));
    }

    // ---------- deleteTask tests ----------

    @Test
    void deleteTask_success() {
        Long eventId = 1L;
        Long taskId = 10L;
        Long tenantId = 100L;
        Long userId = 201L;

        EventDO event = mockEvent(eventId, tenantId, 301L);
        TaskDO task = mockTask(taskId, eventId, userId);

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(taskActionFactory.getStrategy(TaskActionEnum.DELETE)).thenReturn(taskStrategy);
        doNothing().when(taskStrategy).execute(eq(task), isNull(), isNull());

        assertThatCode(() -> service.deleteTask(eventId, taskId)).doesNotThrowAnyException();

        verify(taskActionFactory).getStrategy(TaskActionEnum.DELETE);
        verify(taskStrategy).execute(eq(task), isNull(), isNull());
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

        EventDO event = mockEvent(eventId, tenantId, 301L);

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(taskMapper.selectById(taskId)).thenReturn(null);

        assertThatThrownBy(() -> service.deleteTask(eventId, taskId))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(TASK_NOT_FOUND.getCode());
    }

    // ---------- getTask tests ----------

    @Test
    void getTask_success() {
        Long eventId = 1L;
        Long taskId = 10L;
        Long tenantId = 100L;
        Long assigneeId = 201L;
        Long assignerId = 301L;
        Long assigneeDeptId = 5L;
        Long assignerDeptId = 6L;

        EventDO event = mockEvent(eventId, tenantId, assignerId);
        TaskDO task = mockTask(taskId, eventId, assigneeId);
        UserDO assignee = mockUser(assigneeId, tenantId, assigneeDeptId);
        UserDO assigner = mockUser(assignerId, tenantId, assignerDeptId);
        DeptDO assigneeDept = mockDept(assigneeDeptId, "Assignee Dept");
        DeptDO assignerDept = mockDept(assignerDeptId, "Assigner Dept");

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(userMapper.selectById(assigneeId)).thenReturn(assignee);
        when(userMapper.selectById(assignerId)).thenReturn(assigner);
        when(deptMapper.selectById(assigneeDeptId)).thenReturn(assigneeDept);
        when(deptMapper.selectById(assignerDeptId)).thenReturn(assignerDept);

        TaskRespVO resp = service.getTask(eventId, taskId);

        assertThat(resp).isNotNull();
        assertThat(resp.getId()).isEqualTo(taskId);
        assertThat(resp.getName()).isEqualTo("Test Task");
        assertThat(resp.getAssignerUser()).isNotNull();
        assertThat(resp.getAssignerUser().getId()).isEqualTo(assignerId);
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

        EventDO event = mockEvent(eventId, tenantId, 301L);

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
        Long assignerId = 301L;
        Long assignee1Id = 201L;
        Long assignee2Id = 202L;
        Long dept1Id = 5L;
        Long dept2Id = 6L;

        EventDO event = mockEvent(eventId, tenantId, assignerId);
        TaskDO task1 = mockTask(1L, eventId, assignee1Id);
        TaskDO task2 = mockTask(2L, eventId, assignee2Id);
        UserDO assigner = mockUser(assignerId, tenantId, 7L);
        UserDO assignee1 = mockUser(assignee1Id, tenantId, dept1Id);
        UserDO assignee2 = mockUser(assignee2Id, tenantId, dept2Id);
        DeptDO dept1 = mockDept(dept1Id, "Dept A");
        DeptDO dept2 = mockDept(dept2Id, "Dept B");

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(taskMapper.selectList(any())).thenReturn(List.of(task1, task2));
        when(userMapper.selectById(assignerId)).thenReturn(assigner);
        when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(assignee1, assignee2));
        when(deptMapper.selectBatchIds(anyList())).thenReturn(List.of(dept1, dept2));

        List<TaskRespVO> resp = service.listTasksByEvent(eventId);

        assertThat(resp).hasSize(2);
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

        EventDO event = mockEvent(eventId, tenantId, 301L);

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(taskMapper.selectList(any())).thenReturn(List.of());

        List<TaskRespVO> resp = service.listTasksByEvent(eventId);

        assertThat(resp).isEmpty();
    }

    @Test
    void listTasksByEvent_tasksWithoutUsers_success() {
        Long eventId = 1L;
        Long tenantId = 100L;
        Long assignerId = 301L;

        EventDO event = mockEvent(eventId, tenantId, assignerId);
        TaskDO task1 = mockTask(1L, eventId, null);
        task1.setUserId(null);

        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(taskMapper.selectList(any())).thenReturn(List.of(task1));
        when(userMapper.selectById(assignerId)).thenReturn(null);

        List<TaskRespVO> resp = service.listTasksByEvent(eventId);

        assertThat(resp).hasSize(1);
    }

    // ---------- listTasksByMember tests ----------

    @Test
    void listTasksByMember_success() {
        Long memberId = 201L;
        Long tenantId = 100L;
        Long deptId = 5L;
        Long eventId1 = 1L;
        Long eventId2 = 2L;
        Long assigner1Id = 301L;
        Long assigner2Id = 302L;

        UserDO member = mockUser(memberId, tenantId, deptId);
        TaskDO task1 = mockTask(10L, eventId1, memberId);
        TaskDO task2 = mockTask(11L, eventId2, memberId);
        EventDO event1 = mockEvent(eventId1, tenantId, assigner1Id);
        EventDO event2 = mockEvent(eventId2, tenantId, assigner2Id);
        UserDO assigner1 = mockUser(assigner1Id, tenantId, 7L);
        UserDO assigner2 = mockUser(assigner2Id, tenantId, 8L);
        DeptDO dept = mockDept(deptId, "Dept A");

        when(userMapper.selectById(memberId)).thenReturn(member);
        when(taskMapper.selectList(any())).thenReturn(List.of(task1, task2));
        when(eventMapper.selectBatchIds(anyList())).thenReturn(List.of(event1, event2));
        when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(assigner1, assigner2));
        when(deptMapper.selectBatchIds(anyList())).thenReturn(List.of(dept));

        List<TaskRespVO> resp = service.listTasksByMember(memberId);

        assertThat(resp).hasSize(2);
        assertThat(resp).extracting(TaskRespVO::getId).containsExactly(10L, 11L);
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

    // ---------- getByMemberId tests ----------

    @Test
    void getByMemberId_success() {
        Long memberId = 201L;
        Long tenantId = 100L;
        Long deptId = 5L;
        Long eventId = 1L;

        UserDO member = mockUser(memberId, tenantId, deptId);
        TaskDO task = mockTask(10L, eventId, memberId);
        EventDO event = mockEvent(eventId, tenantId, 301L);
        DeptDO dept = mockDept(deptId, "Dept A");
        dept.setEventId(eventId);
        UserGroupDO userGroup =
                mockUserGroup(memberId, deptId, eventId, LocalDateTime.of(2025, 1, 1, 8, 0));

        when(userMapper.selectById(memberId)).thenReturn(member);
        when(taskMapper.selectList(any())).thenReturn(List.of(task));
        when(eventMapper.selectBatchIds(anyList())).thenReturn(List.of(event));
        when(deptMapper.selectBatchIds(anyList())).thenReturn(List.of(dept));
        when(deptMapper.selectById(deptId)).thenReturn(dept);
        when(eventMapper.selectBatchIds(List.of(eventId))).thenReturn(List.of(event));
        when(userGroupMapper.selectList(any())).thenReturn(List.of(userGroup));
        when(deptMapper.selectBatchIds(List.of(deptId))).thenReturn(List.of(dept));
        when(eventMapper.selectById(eventId)).thenReturn(event);

        TaskDashboardRespVO resp = service.getByMemberId(memberId);

        assertThat(resp).isNotNull();
        assertThat(resp.getMember()).isNotNull();
        assertThat(resp.getMember().getId()).isEqualTo(memberId);
        assertThat(resp.getMember().getUsername()).isEqualTo("User" + memberId);
        assertThat(resp.getGroups()).hasSize(1);
        assertThat(resp.getGroups().get(0).getName()).isEqualTo("Dept A");
        assertThat(resp.getTasks()).hasSize(1);
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

        UserDO member = mockUser(memberId, tenantId, null);

        when(userMapper.selectById(memberId)).thenReturn(member);
        when(taskMapper.selectList(any())).thenReturn(List.of());
        when(userGroupMapper.selectList(any())).thenReturn(List.of());

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
        UserGroupDO userGroup =
                mockUserGroup(memberId, deptId, null, LocalDateTime.of(2025, 1, 1, 8, 0));

        when(userMapper.selectById(memberId)).thenReturn(member);
        when(taskMapper.selectList(any())).thenReturn(List.of());
        when(userGroupMapper.selectList(any())).thenReturn(List.of(userGroup));
        when(deptMapper.selectBatchIds(List.of(deptId))).thenReturn(List.of());

        TaskDashboardRespVO resp = service.getByMemberId(memberId);

        assertThat(resp).isNotNull();
        assertThat(resp.getGroups()).isEmpty();
    }

    // ---------- Private method tests ----------

    @Test
    void fetchUser_nullUserId_returnsNull() {
        UserDO result = ReflectionTestUtils.invokeMethod(service, "fetchUser", new Object[] {null});
        assertThat(result).isNull();
        verifyNoInteractions(userMapper);
    }

    @Test
    void fetchUser_validUserId_returnsUser() {
        Long userId = 201L;
        UserDO user = mockUser(userId, 100L, 5L);

        when(userMapper.selectById(userId)).thenReturn(user);

        UserDO result = ReflectionTestUtils.invokeMethod(service, "fetchUser", userId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
    }

    @Test
    void fetchUsersByIds_emptyCollection_returnsEmptyMap() {
        Map<Long, UserDO> result =
                ReflectionTestUtils.invokeMethod(service, "fetchUsersByIds", List.of());
        assertThat(result).isEmpty();
        verifyNoInteractions(userMapper);
    }

    @Test
    void fetchUsersByIds_nullCollection_returnsEmptyMap() {
        Map<Long, UserDO> result =
                ReflectionTestUtils.invokeMethod(service, "fetchUsersByIds", new Object[] {null});
        assertThat(result).isEmpty();
        verifyNoInteractions(userMapper);
    }

    @Test
    void buildAssignersByEventId_emptyCollection_returnsEmptyMap() {
        Map<Long, UserDO> result =
                ReflectionTestUtils.invokeMethod(service, "buildAssignersByEventId", List.of());
        assertThat(result).isEmpty();
        verifyNoInteractions(userMapper);
    }

    @Test
    void buildAssignersByEventId_validEvents_returnsAssignersMap() {
        Long event1Id = 1L;
        Long event2Id = 2L;
        Long assigner1Id = 301L;
        Long assigner2Id = 302L;

        EventDO event1 = mockEvent(event1Id, 100L, assigner1Id);
        EventDO event2 = mockEvent(event2Id, 100L, assigner2Id);
        UserDO assigner1 = mockUser(assigner1Id, 100L, 5L);
        UserDO assigner2 = mockUser(assigner2Id, 100L, 6L);

        when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(assigner1, assigner2));

        Map<Long, UserDO> result =
                ReflectionTestUtils.invokeMethod(
                        service, "buildAssignersByEventId", List.of(event1, event2));

        assertThat(result).hasSize(2);
        assertThat(result.get(event1Id).getId()).isEqualTo(assigner1Id);
        assertThat(result.get(event2Id).getId()).isEqualTo(assigner2Id);
    }

    @Test
    void buildCrudGroupsByDept_emptyUsersMap_returnsEmptyMap() {
        Map<Long, List<TaskRespVO.AssignedUserVO.GroupVO>> result =
                ReflectionTestUtils.invokeMethod(service, "buildCrudGroupsByDept", Map.of());
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

        when(deptMapper.selectBatchIds(anyList())).thenReturn(List.of(dept1, dept2));

        Map<Long, List<TaskRespVO.AssignedUserVO.GroupVO>> result =
                ReflectionTestUtils.invokeMethod(service, "buildCrudGroupsByDept", usersMap);

        assertThat(result).hasSize(2);
        assertThat(result.get(5L)).hasSize(1);
        assertThat(result.get(5L).get(0).getName()).isEqualTo("Dept A");
    }

    @Test
    void resolveCrudGroups_deptIdNull_returnsEmptyList() {
        List<TaskRespVO.AssignedUserVO.GroupVO> result =
                ReflectionTestUtils.invokeMethod(service, "resolveCrudGroups", null, null);
        assertThat(result).isEmpty();
    }

    @Test
    void resolveAssignerGroups_deptIdNull_returnsEmptyList() {
        List<TaskRespVO.AssignerUserVO.GroupVO> result =
                ReflectionTestUtils.invokeMethod(
                        service, "resolveAssignerGroups", new Object[] {null});
        assertThat(result).isEmpty();
    }

    @Test
    void resolveAssignerGroups_validDeptId_returnsGroups() {
        Long deptId = 5L;
        DeptDO dept = mockDept(deptId, "Test Dept");

        when(deptMapper.selectById(deptId)).thenReturn(dept);

        List<TaskRespVO.AssignerUserVO.GroupVO> result =
                ReflectionTestUtils.invokeMethod(service, "resolveAssignerGroups", deptId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test Dept");
    }

    @Test
    void toMemberVO_convertsCorrectly() {
        Long memberId = 201L;
        UserDO member = mockUser(memberId, 100L, 5L);
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
        UserGroupDO userGroup =
                mockUserGroup(memberId, deptId, eventId, LocalDateTime.of(2025, 1, 1, 8, 0));

        EventDO event = mockEvent(eventId, tenantId);

        when(userGroupMapper.selectList(any())).thenReturn(List.of(userGroup));
        when(deptMapper.selectBatchIds(List.of(deptId))).thenReturn(List.of(dept));
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
        UserDO member = mockUser(201L, 100L, null);

        when(userGroupMapper.selectList(any())).thenReturn(List.of());

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

        UserGroupDO userGroup =
                mockUserGroup(memberId, deptId, null, LocalDateTime.of(2025, 1, 1, 8, 0));

        when(userGroupMapper.selectList(any())).thenReturn(List.of(userGroup));
        when(deptMapper.selectBatchIds(List.of(deptId))).thenReturn(List.of());

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
