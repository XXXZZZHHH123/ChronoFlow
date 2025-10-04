package nus.edu.u.system.service.Task;

import static java.util.stream.Collectors.toMap;
import static nus.edu.u.system.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import nus.edu.u.common.exception.ErrorCode;
import nus.edu.u.common.exception.ServiceException;
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
import nus.edu.u.system.service.task.TaskServiceImpl;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock private TaskMapper taskMapper;
    @Mock private EventMapper eventMapper;
    @Mock private UserMapper userMapper;
    @Mock private DeptMapper deptMapper;

    @InjectMocks private TaskServiceImpl service;

    @Test
    void createTask_success() {
        long eventId = 10L;
        long tenantId = 200L;
        long userId = 30L;
        EventDO event = event(eventId, tenantId);
        when(eventMapper.selectById(eventId)).thenReturn(event);

        TaskCreateReqVO req = baseCreateReq();
        req.setAssignedUserId(userId);
        req.setStatus(TaskStatusEnum.DOING.getStatus());

        UserDO assignee = user(userId, tenantId, 55L);
        assignee.setUsername("Alice");
        when(userMapper.selectById(userId)).thenReturn(assignee);

        DeptDO dept = dept(55L, tenantId, "Tech");
        when(deptMapper.selectById(55L)).thenReturn(dept);

        doAnswer(
                        invocation -> {
                            TaskDO task = invocation.getArgument(0);
                            task.setId(99L);
                            return 1;
                        })
                .when(taskMapper)
                .insert(any(TaskDO.class));

        TaskRespVO resp = service.createTask(eventId, req);

        ArgumentCaptor<TaskDO> taskCaptor = ArgumentCaptor.forClass(TaskDO.class);
        verify(taskMapper).insert(taskCaptor.capture());
        TaskDO inserted = taskCaptor.getValue();
        assertThat(inserted.getEventId()).isEqualTo(eventId);
        assertThat(inserted.getTenantId()).isEqualTo(tenantId);
        assertThat(inserted.getUserId()).isEqualTo(userId);
        assertThat(inserted.getStatus()).isEqualTo(TaskStatusEnum.DOING.getStatus());

        assertThat(resp.getId()).isEqualTo(99L);
        assertThat(resp.getStatus()).isEqualTo(TaskStatusEnum.DOING.getStatus());
        assertThat(resp.getAssignedUser()).isNotNull();
        assertThat(resp.getAssignedUser().getName()).isEqualTo("Alice");
        assertThat(resp.getAssignedUser().getEmail()).isEqualTo(assignee.getEmail());
        assertThat(resp.getAssignedUser().getPhone()).isEqualTo(assignee.getPhone());
        assertThat(resp.getAssignedUser().getGroups())
                .singleElement()
                .satisfies(
                        group -> {
                            assertThat(group.getId()).isEqualTo(55L);
                            assertThat(group.getName()).isEqualTo("Tech");
                        });
        assertThat(resp.getEvent()).isNotNull();
        assertThat(resp.getEvent().getId()).isEqualTo(eventId);
    }

    @Test
    void createTask_eventNotFound_throws() {
        assertThrowsCode(() -> service.createTask(1L, baseCreateReq()), EVENT_NOT_FOUND);
    }

    @Test
    void createTask_assigneeMissing_throws() {
        long eventId = 1L;
        when(eventMapper.selectById(eventId)).thenReturn(event(eventId, 99L));

        TaskCreateReqVO req = baseCreateReq();
        req.setAssignedUserId(50L);

        when(userMapper.selectById(50L)).thenReturn(null);

        assertThrowsCode(() -> service.createTask(eventId, req), TASK_ASSIGNEE_NOT_FOUND);
    }

    @Test
    void createTask_assigneeTenantMismatch_throws() {
        long eventId = 1L;
        EventDO event = event(eventId, 1L);
        when(eventMapper.selectById(eventId)).thenReturn(event);

        TaskCreateReqVO req = baseCreateReq();
        req.setAssignedUserId(77L);

        UserDO assignee = user(77L, 2L, 10L);
        when(userMapper.selectById(77L)).thenReturn(assignee);

        assertThrowsCode(() -> service.createTask(eventId, req), TASK_ASSIGNEE_TENANT_MISMATCH);
    }

    @Test
    void createTask_invalidTimeRange_throws() {
        long eventId = 1L;
        when(eventMapper.selectById(eventId)).thenReturn(event(eventId, 11L));

        TaskCreateReqVO req = baseCreateReq();
        req.setAssignedUserId(55L);
        req.setStartTime(LocalDateTime.of(2025, 1, 1, 12, 0));
        req.setEndTime(LocalDateTime.of(2025, 1, 1, 12, 0));

        when(userMapper.selectById(anyLong())).thenReturn(user(55L, 11L, 20L));

        assertThrowsCode(() -> service.createTask(eventId, req), TASK_TIME_RANGE_INVALID);
    }

    @Test
    void createTask_startBeforeEvent_throws() {
        long eventId = 1L;
        EventDO event = event(eventId, 11L);
        event.setStartTime(LocalDateTime.of(2025, 1, 1, 9, 0));
        when(eventMapper.selectById(eventId)).thenReturn(event);

        TaskCreateReqVO req = baseCreateReq();
        req.setAssignedUserId(55L);
        req.setStartTime(LocalDateTime.of(2025, 1, 1, 8, 0));
        req.setEndTime(LocalDateTime.of(2025, 1, 1, 10, 0));

        when(userMapper.selectById(anyLong())).thenReturn(user(55L, 11L, 20L));

        assertThrowsCode(() -> service.createTask(eventId, req), TASK_TIME_OUTSIDE_EVENT);
    }

    @Test
    void updateTask_endAfterEvent_throws() {
        long eventId = 2L;
        long taskId = 5L;
        EventDO event = event(eventId, 44L);
        event.setEndTime(LocalDateTime.of(2025, 6, 1, 12, 0));
        when(eventMapper.selectById(eventId)).thenReturn(event);

        TaskDO task =
                TaskDO.builder()
                        .id(taskId)
                        .eventId(eventId)
                        .status(TaskStatusEnum.WAITING.getStatus())
                        .startTime(LocalDateTime.of(2025, 6, 1, 8, 0))
                        .endTime(LocalDateTime.of(2025, 6, 1, 10, 0))
                        .build();
        task.setTenantId(44L);
        task.setUserId(90L);
        when(taskMapper.selectById(taskId)).thenReturn(task);

        TaskUpdateReqVO req = new TaskUpdateReqVO();
        req.setEndTime(LocalDateTime.of(2025, 6, 1, 13, 0));

        when(userMapper.selectById(90L)).thenReturn(user(90L, 44L, 12L));

        assertThrowsCode(() -> service.updateTask(eventId, taskId, req), TASK_TIME_OUTSIDE_EVENT);
    }

    @Test
    void createTask_invalidStatus_throws() {
        long eventId = 1L;
        when(eventMapper.selectById(eventId)).thenReturn(event(eventId, 10L));

        TaskCreateReqVO req = baseCreateReq();
        req.setAssignedUserId(8L);
        req.setStatus(99);

        when(userMapper.selectById(anyLong())).thenReturn(user(8L, 10L, 1L));

        assertThrowsCode(() -> service.createTask(eventId, req), TASK_STATUS_INVALID);
    }

    @Test
    void updateTask_eventNotFound_throws() {
        TaskUpdateReqVO req = new TaskUpdateReqVO();
        assertThrowsCode(() -> service.updateTask(1L, 2L, req), EVENT_NOT_FOUND);
    }

    @Test
    void updateTask_taskMissing_throws() {
        long eventId = 1L;
        when(eventMapper.selectById(eventId)).thenReturn(event(eventId, 1L));
        when(taskMapper.selectById(2L)).thenReturn(null);

        assertThrowsCode(
                () -> service.updateTask(eventId, 2L, new TaskUpdateReqVO()), TASK_NOT_FOUND);
    }

    @Test
    void updateTask_taskBelongsToAnotherEvent_throws() {
        long eventId = 1L;
        when(eventMapper.selectById(eventId)).thenReturn(event(eventId, 1L));
        TaskDO foreignTask = TaskDO.builder().id(3L).eventId(999L).build();
        foreignTask.setTenantId(1L);
        when(taskMapper.selectById(3L)).thenReturn(foreignTask);

        assertThrowsCode(
                () -> service.updateTask(eventId, 3L, new TaskUpdateReqVO()), TASK_NOT_FOUND);
    }

    @Test
    void updateTask_assigneeNotFound_throws() {
        long eventId = 1L;
        long taskId = 2L;
        when(eventMapper.selectById(eventId)).thenReturn(event(eventId, 5L));
        TaskDO task = TaskDO.builder().id(taskId).eventId(eventId).build();
        task.setTenantId(5L);
        when(taskMapper.selectById(taskId)).thenReturn(task);

        TaskUpdateReqVO req = new TaskUpdateReqVO();
        req.setAssignedUserId(90L);

        when(userMapper.selectById(90L)).thenReturn(null);

        assertThrowsCode(() -> service.updateTask(eventId, taskId, req), TASK_ASSIGNEE_NOT_FOUND);
    }

    @Test
    void updateTask_assigneeTenantMismatch_throws() {
        long eventId = 1L;
        long taskId = 2L;
        EventDO event = event(eventId, 5L);
        when(eventMapper.selectById(eventId)).thenReturn(event);
        TaskDO task = TaskDO.builder().id(taskId).eventId(eventId).build();
        task.setTenantId(5L);
        when(taskMapper.selectById(taskId)).thenReturn(task);

        TaskUpdateReqVO req = new TaskUpdateReqVO();
        req.setAssignedUserId(90L);

        when(userMapper.selectById(90L)).thenReturn(user(90L, 999L, 1L));

        assertThrowsCode(
                () -> service.updateTask(eventId, taskId, req), TASK_ASSIGNEE_TENANT_MISMATCH);
    }

    @Test
    void updateTask_invalidStatus_throws() {
        long eventId = 1L;
        long taskId = 2L;
        EventDO event = event(eventId, 5L);
        when(eventMapper.selectById(eventId)).thenReturn(event);
        TaskDO task =
                TaskDO.builder()
                        .id(taskId)
                        .eventId(eventId)
                        .status(TaskStatusEnum.WAITING.getStatus())
                        .build();
        task.setTenantId(5L);
        when(taskMapper.selectById(taskId)).thenReturn(task);

        TaskUpdateReqVO req = new TaskUpdateReqVO();
        req.setStatus(42);

        assertThrowsCode(() -> service.updateTask(eventId, taskId, req), TASK_STATUS_INVALID);
    }

    @Test
    void updateTask_success_updatesFields() {
        long eventId = 4L;
        long taskId = 8L;
        long tenantId = 77L;
        EventDO event = event(eventId, tenantId);
        when(eventMapper.selectById(eventId)).thenReturn(event);

        TaskDO existing =
                TaskDO.builder()
                        .id(taskId)
                        .eventId(eventId)
                        .userId(11L)
                        .name("Old")
                        .description("Old Desc")
                        .status(TaskStatusEnum.WAITING.getStatus())
                        .startTime(LocalDateTime.of(2025, 1, 1, 9, 0))
                        .endTime(LocalDateTime.of(2025, 1, 1, 10, 0))
                        .build();
        existing.setTenantId(tenantId);
        when(taskMapper.selectById(taskId)).thenReturn(existing);

        long newUserId = 22L;
        TaskUpdateReqVO req = new TaskUpdateReqVO();
        req.setName("New Name");
        req.setDescription("New Desc");
        req.setStatus(TaskStatusEnum.DONE.getStatus());
        req.setStartTime(LocalDateTime.of(2025, 1, 1, 11, 0));
        req.setEndTime(LocalDateTime.of(2025, 1, 1, 12, 0));
        req.setAssignedUserId(newUserId);

        UserDO assignee = user(newUserId, tenantId, 66L);
        assignee.setUsername("Bob");
        when(userMapper.selectById(newUserId)).thenReturn(assignee);
        when(deptMapper.selectById(66L)).thenReturn(dept(66L, tenantId, "Ops"));

        when(taskMapper.updateById(existing)).thenReturn(1);

        TaskRespVO resp = service.updateTask(eventId, taskId, req);

        assertThat(existing.getName()).isEqualTo("New Name");
        assertThat(existing.getDescription()).isEqualTo("New Desc");
        assertThat(existing.getStatus()).isEqualTo(TaskStatusEnum.DONE.getStatus());
        assertThat(existing.getStartTime()).isEqualTo(req.getStartTime());
        assertThat(existing.getEndTime()).isEqualTo(req.getEndTime());
        assertThat(existing.getUserId()).isEqualTo(newUserId);
        assertThat(existing.getTenantId()).isEqualTo(tenantId);

        verify(taskMapper).updateById(existing);

        assertThat(resp.getAssignedUser()).isNotNull();
        assertThat(resp.getAssignedUser().getName()).isEqualTo("Bob");
        assertThat(resp.getAssignedUser().getGroups()).hasSize(1);
        assertThat(resp.getEvent()).isNotNull();
        assertThat(resp.getEvent().getId()).isEqualTo(eventId);
    }

    @Test
    void deleteTask_eventNotFound_throws() {
        assertThrowsCode(() -> service.deleteTask(1L, 2L), EVENT_NOT_FOUND);
    }

    @Test
    void deleteTask_taskMissing_throws() {
        long eventId = 1L;
        when(eventMapper.selectById(eventId)).thenReturn(event(eventId, 1L));
        when(taskMapper.selectById(2L)).thenReturn(null);

        assertThrowsCode(() -> service.deleteTask(eventId, 2L), TASK_NOT_FOUND);
    }

    @Test
    void deleteTask_success() {
        long eventId = 3L;
        long taskId = 9L;
        EventDO event = event(eventId, 12L);
        when(eventMapper.selectById(eventId)).thenReturn(event);
        TaskDO task = TaskDO.builder().id(taskId).eventId(eventId).build();
        task.setTenantId(12L);
        when(taskMapper.selectById(taskId)).thenReturn(task);

        service.deleteTask(eventId, taskId);

        verify(taskMapper).deleteById(taskId);
    }

    @Test
    void getTask_eventNotFound_throws() {
        assertThrowsCode(() -> service.getTask(1L, 2L), EVENT_NOT_FOUND);
    }

    @Test
    void getTask_taskMissing_throws() {
        long eventId = 5L;
        when(eventMapper.selectById(eventId)).thenReturn(event(eventId, 1L));
        when(taskMapper.selectById(2L)).thenReturn(null);

        assertThrowsCode(() -> service.getTask(eventId, 2L), TASK_NOT_FOUND);
    }

    @Test
    void getTask_success_fetchesAssignedUser() {
        long eventId = 7L;
        long taskId = 11L;
        long tenantId = 91L;
        EventDO event = event(eventId, tenantId);
        when(eventMapper.selectById(eventId)).thenReturn(event);

        TaskDO task =
                TaskDO.builder()
                        .id(taskId)
                        .eventId(eventId)
                        .userId(50L)
                        .name("Demo")
                        .status(TaskStatusEnum.DOING.getStatus())
                        .startTime(LocalDateTime.of(2025, 3, 1, 9, 0))
                        .endTime(LocalDateTime.of(2025, 3, 1, 11, 0))
                        .build();
        task.setTenantId(tenantId);
        when(taskMapper.selectById(taskId)).thenReturn(task);

        UserDO user = user(50L, tenantId, 70L);
        user.setUsername("Carol");
        when(userMapper.selectById(50L)).thenReturn(user);
        when(deptMapper.selectById(70L)).thenReturn(dept(70L, tenantId, "HR"));

        TaskRespVO resp = service.getTask(eventId, taskId);

        assertThat(resp.getAssignedUser()).isNotNull();
        assertThat(resp.getAssignedUser().getName()).isEqualTo("Carol");
        assertThat(resp.getAssignedUser().getGroups())
                .singleElement()
                .satisfies(group -> assertThat(group.getName()).isEqualTo("HR"));
        assertThat(resp.getEvent()).isNotNull();
        assertThat(resp.getEvent().getId()).isEqualTo(eventId);
    }

    @Test
    void listTasksByEvent_eventNotFound_throws() {
        assertThrowsCode(() -> service.listTasksByEvent(1L), EVENT_NOT_FOUND);
    }

    @Test
    void listTasksByEvent_noTasks_returnsEmptyList() {
        long eventId = 2L;
        when(eventMapper.selectById(eventId)).thenReturn(event(eventId, 3L));
        when(taskMapper.selectList(any())).thenReturn(List.of());

        List<TaskRespVO> result = service.listTasksByEvent(eventId);

        assertThat(result).isEmpty();
        verifyNoInteractions(userMapper, deptMapper);
    }

    @Test
    void listTasksByEvent_populatesAssigneesAndGroups() {
        long eventId = 3L;
        when(eventMapper.selectById(eventId)).thenReturn(event(eventId, 4L));

        TaskDO task1 =
                TaskDO.builder()
                        .id(101L)
                        .eventId(eventId)
                        .userId(11L)
                        .name("Task A")
                        .description("Desc A")
                        .status(TaskStatusEnum.DOING.getStatus())
                        .startTime(LocalDateTime.of(2025, 4, 1, 9, 0))
                        .endTime(LocalDateTime.of(2025, 4, 1, 10, 0))
                        .build();
        TaskDO task2 =
                TaskDO.builder()
                        .id(102L)
                        .eventId(eventId)
                        .userId(22L)
                        .name("Task B")
                        .description("Desc B")
                        .status(TaskStatusEnum.WAITING.getStatus())
                        .startTime(LocalDateTime.of(2025, 4, 1, 11, 0))
                        .endTime(LocalDateTime.of(2025, 4, 1, 12, 0))
                        .build();
        TaskDO task3 =
                TaskDO.builder()
                        .id(103L)
                        .eventId(eventId)
                        .userId(null)
                        .name("Task C")
                        .description("Desc C")
                        .status(TaskStatusEnum.DONE.getStatus())
                        .startTime(LocalDateTime.of(2025, 4, 1, 13, 0))
                        .endTime(LocalDateTime.of(2025, 4, 1, 14, 0))
                        .build();
        when(taskMapper.selectList(any())).thenReturn(List.of(task1, task2, task3));

        UserDO user1 = user(11L, 4L, 501L);
        user1.setUsername("Alex");
        UserDO user2 = user(22L, 4L, 502L);
        user2.setUsername("Bea");
        when(userMapper.selectBatchIds(anyCollection()))
                .thenAnswer(
                        invocation -> {
                            @SuppressWarnings("unchecked")
                            Collection<Long> ids = (Collection<Long>) invocation.getArgument(0);
                            assertThat(ids).containsExactlyInAnyOrder(11L, 22L);
                            return List.of(user1, user2);
                        });

        when(deptMapper.selectBatchIds(anyCollection()))
                .thenAnswer(
                        invocation -> {
                            @SuppressWarnings("unchecked")
                            Collection<Long> ids = (Collection<Long>) invocation.getArgument(0);
                            assertThat(ids).containsExactlyInAnyOrder(501L, 502L);
                            return List.of(dept(501L, 4L, "Dept-501"));
                        });
        when(deptMapper.selectById(502L)).thenReturn(dept(502L, 4L, "Dept-502"));

        List<TaskRespVO> resp = service.listTasksByEvent(eventId);

        assertThat(resp).hasSize(3);

        Map<Long, TaskRespVO> byId = resp.stream().collect(toMap(TaskRespVO::getId, v -> v));

        TaskRespVO first = byId.get(101L);
        assertThat(first.getEvent()).isNotNull();
        assertThat(first.getAssignedUser().getName()).isEqualTo("Alex");
        assertThat(first.getAssignedUser().getGroups())
                .extracting(TaskRespVO.AssignedUserVO.GroupVO::getName)
                .containsExactly("Dept-501");

        TaskRespVO second = byId.get(102L);
        assertThat(second.getEvent()).isNotNull();
        assertThat(second.getAssignedUser().getName()).isEqualTo("Bea");
        assertThat(second.getAssignedUser().getGroups())
                .extracting(TaskRespVO.AssignedUserVO.GroupVO::getName)
                .containsExactly("Dept-502");

        TaskRespVO third = byId.get(103L);
        assertThat(third.getEvent()).isNotNull();
        assertThat(third.getAssignedUser()).isNull();
    }

    @Test
    void listTasksByEvent_usersWithoutDept_returnEmptyGroups() {
        long eventId = 4L;
        when(eventMapper.selectById(eventId)).thenReturn(event(eventId, 5L));

        TaskDO task =
                TaskDO.builder()
                        .id(201L)
                        .eventId(eventId)
                        .userId(77L)
                        .name("Task No Dept")
                        .status(TaskStatusEnum.WAITING.getStatus())
                        .build();
        when(taskMapper.selectList(any())).thenReturn(List.of(task));

        UserDO user = user(77L, 5L, null);
        user.setUsername("NoDept");
        when(userMapper.selectBatchIds(List.of(77L))).thenReturn(List.of(user));

        List<TaskRespVO> resp = service.listTasksByEvent(eventId);

        assertThat(resp)
                .singleElement()
                .satisfies(
                        vo -> {
                            assertThat(vo.getEvent()).isNotNull();
                            assertThat(vo.getAssignedUser().getGroups()).isEmpty();
                        });

        verify(deptMapper, never()).selectBatchIds(anyList());
    }

    @Test
    void listTasksByMember_userNotFound_throws() {
        when(userMapper.selectById(88L)).thenReturn(null);

        assertThrowsCode(() -> service.listTasksByMember(88L), USER_NOT_FOUND);
    }

    @Test
    void listTasksByMember_returnsEnrichedTasks() {
        long memberId = 9L;
        long tenantId = 44L;
        long deptId = 501L;

        UserDO member = user(memberId, tenantId, deptId);
        member.setUsername("Member");
        when(userMapper.selectById(memberId)).thenReturn(member);

        TaskDO firstTask =
                TaskDO.builder().id(301L).eventId(1001L).userId(memberId).name("First").build();
        TaskDO secondTask =
                TaskDO.builder().id(302L).eventId(1002L).userId(memberId).name("Second").build();
        when(taskMapper.selectList(any())).thenReturn(List.of(firstTask, secondTask));

        DeptDO dept = dept(deptId, tenantId, "Dev Team");
        dept.setEventId(1001L);
        when(deptMapper.selectBatchIds(List.of(deptId))).thenReturn(List.of(dept));

        EventDO eventOne = event(1001L, tenantId);
        eventOne.setName("Hackathon");
        EventDO eventTwo = event(1002L, tenantId);
        eventTwo.setName("Workshop");
        when(eventMapper.selectBatchIds(anyCollection())).thenReturn(List.of(eventOne, eventTwo));

        List<TaskRespVO> resp = service.listTasksByMember(memberId);

        assertThat(resp).extracting(TaskRespVO::getId).containsExactlyInAnyOrder(301L, 302L);
        assertThat(resp)
                .allSatisfy(
                        vo -> {
                            assertThat(vo.getAssignedUser()).isNotNull();
                            assertThat(vo.getAssignedUser().getId()).isEqualTo(memberId);
                            assertThat(vo.getAssignedUser().getGroups())
                                    .extracting(TaskRespVO.AssignedUserVO.GroupVO::getName)
                                    .containsExactly("Dev Team");
                            assertThat(vo.getEvent()).isNotNull();
                        });

        Map<Long, TaskRespVO> byId =
                resp.stream().collect(toMap(TaskRespVO::getId, value -> value));
        assertThat(byId.get(301L).getEvent().getName()).isEqualTo("Hackathon");
        assertThat(byId.get(302L).getEvent().getName()).isEqualTo("Workshop");
    }

    @Test
    void getByMemberId_userNotFound_throws() {
        when(userMapper.selectById(77L)).thenReturn(null);

        assertThrowsCode(() -> service.getByMemberId(77L), USER_NOT_FOUND);
    }

    @Test
    void getByMemberId_returnsDashboard() {
        long memberId = 10L;
        long tenantId = 55L;
        long deptId = 888L;

        UserDO member = user(memberId, tenantId, deptId);
        member.setUsername("DashboardUser");
        when(userMapper.selectById(memberId)).thenReturn(member);

        DeptDO dept = dept(deptId, tenantId, "Analytics");
        dept.setEventId(2001L);
        dept.setSort(3);
        dept.setRemark("Important group");
        when(deptMapper.selectById(deptId)).thenReturn(dept);

        EventDO linkedEvent = event(2001L, tenantId);
        linkedEvent.setName("Conference");
        when(eventMapper.selectById(2001L)).thenReturn(linkedEvent);

        TaskDO task =
                TaskDO.builder().id(400L).eventId(2001L).userId(memberId).name("Prep").build();
        when(taskMapper.selectList(any())).thenReturn(List.of(task));
        when(eventMapper.selectBatchIds(anyCollection())).thenReturn(List.of(linkedEvent));
        when(deptMapper.selectBatchIds(List.of(deptId))).thenReturn(List.of(dept));

        TaskDashboardRespVO dashboard = service.getByMemberId(memberId);

        assertThat(dashboard.getMember()).isNotNull();
        assertThat(dashboard.getMember().getId()).isEqualTo(memberId);
        assertThat(dashboard.getMember().getUsername()).isEqualTo("DashboardUser");

        assertThat(dashboard.getGroups())
                .singleElement()
                .satisfies(
                        group -> {
                            assertThat(group.getName()).isEqualTo("Analytics");
                            assertThat(group.getEvent()).isNotNull();
                            assertThat(group.getEvent().getName()).isEqualTo("Conference");
                        });

        assertThat(dashboard.getTasks()).hasSize(1);
        TaskRespVO taskResp = dashboard.getTasks().get(0);
        assertThat(taskResp.getId()).isEqualTo(400L);
        assertThat(taskResp.getEvent()).isNotNull();
    }

    private TaskCreateReqVO baseCreateReq() {
        TaskCreateReqVO req = new TaskCreateReqVO();
        req.setName("Task");
        req.setDescription("Do something");
        req.setStartTime(LocalDateTime.of(2025, 1, 1, 9, 0));
        req.setEndTime(LocalDateTime.of(2025, 1, 1, 10, 0));
        req.setAssignedUserId(1L);
        return req;
    }

    private EventDO event(Long id, Long tenantId) {
        EventDO event =
                EventDO.builder()
                        .id(id)
                        .name("Event")
                        .description("Desc")
                        .location("LT1")
                        .startTime(LocalDateTime.of(2025, 1, 1, 9, 0))
                        .endTime(LocalDateTime.of(2025, 1, 1, 17, 0))
                        .status(1)
                        .remark("remark")
                        .build();
        event.setTenantId(tenantId);
        return event;
    }

    private UserDO user(Long id, Long tenantId, Long deptId) {
        UserDO user = UserDO.builder().id(id).deptId(deptId).username("user" + id).build();
        user.setTenantId(tenantId);
        user.setEmail("user" + id + "@example.com");
        user.setPhone("+65" + String.format("%08d", id));
        return user;
    }

    private DeptDO dept(Long id, Long tenantId, String name) {
        DeptDO dept = DeptDO.builder().id(id).name(name).build();
        dept.setTenantId(tenantId);
        return dept;
    }

    private void assertThrowsCode(ThrowableAssert.ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call)
                .isInstanceOf(ServiceException.class)
                .satisfies(
                        ex -> {
                            ServiceException se = (ServiceException) ex;
                            assertThat(se.getCode()).isEqualTo(code.getCode());
                            assertThat(se.getMessage()).contains(code.getMsg());
                        });
    }
}
