package nus.edu.u.system.service.Event;

import static nus.edu.u.system.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.system.enums.event.EventStatusEnum;
import nus.edu.u.common.exception.ErrorCode;
import nus.edu.u.common.exception.ServiceException;
import nus.edu.u.system.domain.dataobject.dept.DeptDO;
import nus.edu.u.system.domain.dataobject.task.EventDO;
import nus.edu.u.system.domain.dataobject.task.TaskDO;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.dataobject.user.UserGroupDO;
import nus.edu.u.system.domain.vo.event.*;
import nus.edu.u.system.enums.ErrorCodeConstants;
import nus.edu.u.system.enums.task.TaskStatusEnum;
import nus.edu.u.system.mapper.dept.DeptMapper;
import nus.edu.u.system.mapper.task.EventMapper;
import nus.edu.u.system.mapper.task.TaskMapper;
import nus.edu.u.system.mapper.user.UserGroupMapper;
import nus.edu.u.system.mapper.user.UserMapper;
import nus.edu.u.system.service.event.EventServiceImpl;
import nus.edu.u.system.service.group.GroupService;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock private EventMapper eventMapper;
    @Mock private UserGroupMapper userGroupMapper;
    @Mock private UserMapper userMapper;
    @Mock private DeptMapper deptMapper;
    @Mock private TaskMapper taskMapper;
    @Mock private GroupService groupService;

    @InjectMocks private EventServiceImpl service;

    private EventCreateReqVO newCreateReq() {
        EventCreateReqVO vo = new EventCreateReqVO();
        vo.setEventName("NUS Hackathon");
        vo.setDescription("Annual hackathon for students");
        vo.setOrganizerId(111L);
        vo.setLocation("UTown Hall");
        vo.setStartTime(LocalDateTime.of(2025, 11, 15, 9, 0, 0));
        vo.setEndTime(LocalDateTime.of(2025, 11, 15, 17, 0, 0));
        vo.setParticipantUserIds(List.of(201L, 202L));
        return vo;
    }

    private EventDO persistedEvent(Long id) {
        EventDO event =
                EventDO.builder()
                        .id(id)
                        .userId(111L)
                        .name("NUS Hackathon")
                        .description("Annual hackathon for students")
                        .location("UTown Hall")
                        .startTime(LocalDateTime.of(2025, 11, 15, 9, 0))
                        .endTime(LocalDateTime.of(2025, 11, 15, 17, 0))
                        .status(EventStatusEnum.ACTIVE.getCode())
                        .build();
        event.setCreateTime(LocalDateTime.of(2025, 1, 1, 0, 0).plusMinutes(id));
        return event;
    }

    @Test
    void createEvent_success() {
        when(userMapper.selectById(111L)).thenReturn(UserDO.builder().id(111L).build());
        when(userMapper.selectBatchIds(List.of(201L, 202L)))
                .thenReturn(
                        List.of(
                                UserDO.builder().id(201L).build(),
                                UserDO.builder().id(202L).build()));
        when(eventMapper.insert(any(EventDO.class)))
                .thenAnswer(
                        invocation -> {
                            EventDO inserted = invocation.getArgument(0);
                            inserted.setId(1L);
                            return 1;
                        });
        when(userGroupMapper.selectList(any())).thenReturn(List.of());

        EventRespVO resp = service.createEvent(newCreateReq());

        assertThat(resp.getName()).isEqualTo("NUS Hackathon");
        assertThat(resp.getLocation()).isEqualTo("UTown Hall");
        assertThat(resp.getJoiningParticipants()).isZero();
        assertThat(resp.getStatus()).isEqualTo(EventStatusEnum.ACTIVE.getCode());

        verify(eventMapper).insert(any(EventDO.class));
        verify(userGroupMapper).selectList(any());
    }

    @Test
    void createEvent_fail_whenOrganizerMissing() {
        EventCreateReqVO req = newCreateReq();
        when(userMapper.selectById(111L)).thenReturn(null);

        assertThatThrownBy(() -> service.createEvent(req))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(ErrorCodeConstants.ORGANIZER_NOT_FOUND.getCode());
    }

    @Test
    void createEvent_timeInvalid_throws() {
        EventCreateReqVO req = new EventCreateReqVO();
        req.setOrganizerId(111L);
        req.setStartTime(LocalDateTime.of(2025, 1, 1, 10, 0));
        req.setEndTime(LocalDateTime.of(2025, 1, 1, 10, 0));

        assertThatThrownBy(() -> service.createEvent(req))
                .hasMessageContaining(TIME_RANGE_INVALID.getMsg());
    }

    @Test
    void createEvent_participantsDuplicate_throws() {
        EventCreateReqVO req = new EventCreateReqVO();
        req.setOrganizerId(111L);
        req.setStartTime(LocalDateTime.now());
        req.setEndTime(LocalDateTime.now().plusHours(1));
        req.setParticipantUserIds(List.of(301L, 301L));
        when(userMapper.selectById(111L)).thenReturn(UserDO.builder().id(111L).build());

        assertThatThrownBy(() -> service.createEvent(req))
                .hasMessageContaining(DUPLICATE_PARTICIPANTS.getMsg());
    }

    @Test
    void createEvent_participantsMissing_throws() {
        EventCreateReqVO req = new EventCreateReqVO();
        req.setOrganizerId(111L);
        req.setStartTime(LocalDateTime.now());
        req.setEndTime(LocalDateTime.now().plusHours(1));
        req.setParticipantUserIds(List.of(301L, 999L));

        when(userMapper.selectById(111L)).thenReturn(UserDO.builder().id(111L).build());
        when(userMapper.selectBatchIds(List.of(301L, 999L)))
                .thenReturn(List.of(UserDO.builder().id(301L).build()));

        assertThatThrownBy(() -> service.createEvent(req))
                .hasMessageContaining(PARTICIPANT_NOT_FOUND.getMsg());
    }

    @Test
    void getByEventId_missing_throws() {
        when(eventMapper.selectById(1L)).thenReturn(null);

        assertThrowsCode(() -> service.getByEventId(1L), EVENT_NOT_FOUND);
    }

    @Test
    void getByEventId_success_populatesAggregates() {
        long eventId = 42L;
        EventDO event = persistedEvent(eventId);
        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(userGroupMapper.selectList(any()))
                .thenReturn(
                        List.of(
                                UserGroupDO.builder().eventId(eventId).userId(1L).build(),
                                UserGroupDO.builder().eventId(eventId).userId(2L).build(),
                                UserGroupDO.builder().eventId(eventId).userId(3L).build()));
        when(deptMapper.selectList(any()))
                .thenReturn(
                        List.of(
                                DeptDO.builder()
                                        .id(10L)
                                        .eventId(eventId)
                                        .name("Alpha")
                                        .status(CommonStatusEnum.ENABLE.getStatus())
                                        .build()));
        when(taskMapper.selectList(any()))
                .thenReturn(
                        List.of(
                                TaskDO.builder()
                                        .eventId(eventId)
                                        .status(TaskStatusEnum.COMPLETED.getStatus())
                                        .build(),
                                TaskDO.builder()
                                        .eventId(eventId)
                                        .status(TaskStatusEnum.PROGRESS.getStatus())
                                        .build()));

        EventRespVO resp = service.getByEventId(eventId);

        assertThat(resp.getId()).isEqualTo(eventId);
        assertThat(resp.getJoiningParticipants()).isEqualTo(3);
        assertThat(resp.getGroups())
                .singleElement()
                .extracting(EventRespVO.GroupVO::getName)
                .isEqualTo("Alpha");
        assertThat(resp.getTaskStatus().getTotal()).isEqualTo(2);
        assertThat(resp.getTaskStatus().getCompleted()).isEqualTo(1);
        assertThat(resp.getTaskStatus().getRemaining()).isEqualTo(1);
    }

    @Test
    void getByEventId_success_withoutTasksUsesEmptyStatus() {
        long eventId = 43L;
        when(eventMapper.selectById(eventId)).thenReturn(persistedEvent(eventId));
        when(userGroupMapper.selectList(any())).thenReturn(List.of());
        when(deptMapper.selectList(any())).thenReturn(List.of());
        when(taskMapper.selectList(any())).thenReturn(List.of());

        EventRespVO resp = service.getByEventId(eventId);

        assertThat(resp.getTaskStatus().getTotal()).isZero();
        assertThat(resp.getTaskStatus().getCompleted()).isZero();
        assertThat(resp.getTaskStatus().getRemaining()).isZero();
    }

    @Test
    void fetchGroupsByEventIds_null_returnsEmptyMap() {
        Map<Long, List<EventRespVO.GroupVO>> result =
                ReflectionTestUtils.invokeMethod(service, "fetchGroupsByEventIds", (Object) null);

        assertThat(result).isEmpty();
        verifyNoInteractions(deptMapper);
    }

    @Test
    void fetchGroupsByEventIds_empty_returnsEmptyMap() {
        Map<Long, List<EventRespVO.GroupVO>> result =
                ReflectionTestUtils.invokeMethod(service, "fetchGroupsByEventIds", List.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(deptMapper);
    }

    @Test
    void fetchTaskStatusesByEventIds_null_returnsEmptyMap() {
        Map<Long, EventRespVO.TaskStatusVO> result =
                ReflectionTestUtils.invokeMethod(
                        service, "fetchTaskStatusesByEventIds", (Object) null);

        assertThat(result).isEmpty();
        verifyNoInteractions(taskMapper);
    }

    @Test
    void fetchTaskStatusesByEventIds_empty_returnsEmptyMap() {
        Map<Long, EventRespVO.TaskStatusVO> result =
                ReflectionTestUtils.invokeMethod(service, "fetchTaskStatusesByEventIds", List.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(taskMapper);
    }

    @Test
    void fetchTaskStatusesByEventIds_missingTasks_returnsEmptyStatusPerEvent() {
        List<Long> eventIds = List.of(1L, 2L);
        when(taskMapper.selectList(any()))
                .thenReturn(
                        List.of(
                                TaskDO.builder()
                                        .eventId(1L)
                                        .status(TaskStatusEnum.COMPLETED.getStatus())
                                        .build()));

        Map<Long, EventRespVO.TaskStatusVO> result =
                ReflectionTestUtils.invokeMethod(service, "fetchTaskStatusesByEventIds", eventIds);

        assertThat(result.get(1L).getTotal()).isEqualTo(1);
        assertThat(result.get(2L).getTotal()).isZero();
    }

    @Test
    void updateEvent_success_patchAndReadsParticipantsFromGroups() {
        Long id = 7L;
        when(eventMapper.selectById(id)).thenReturn(persistedEvent(id));
        when(userMapper.selectById(222L)).thenReturn(UserDO.builder().id(222L).build());
        when(userMapper.selectBatchIds(List.of(301L, 303L)))
                .thenReturn(
                        List.of(
                                UserDO.builder().id(301L).build(),
                                UserDO.builder().id(303L).build()));
        when(eventMapper.updateById(any(EventDO.class))).thenReturn(1);
        when(eventMapper.selectById(id))
                .thenReturn(persistedEvent(id).toBuilder().userId(222L).remark("patched").build());
        when(userGroupMapper.selectList(any()))
                .thenReturn(
                        List.of(
                                UserGroupDO.builder().eventId(id).userId(301L).build(),
                                UserGroupDO.builder().eventId(id).userId(303L).build()));

        EventUpdateReqVO req = new EventUpdateReqVO();
        req.setOrganizerId(222L);
        req.setRemark("patched");
        req.setParticipantUserIds(List.of(301L, 303L));

        UpdateEventRespVO resp = service.updateEvent(id, req);

        assertThat(resp.getOrganizerId()).isEqualTo(222L);
        assertThat(resp.getRemarks()).isEqualTo("patched");
        assertThat(resp.getParticipantUserIds()).containsExactlyInAnyOrder(301L, 303L);

        verify(userGroupMapper).selectList(any());
    }

    @Test
    void updateEvent_timeInvalid_throws() {
        Long id = 7L;
        EventDO db = persistedEvent(id);
        when(eventMapper.selectById(id)).thenReturn(db);

        EventUpdateReqVO req = new EventUpdateReqVO();
        req.setStartTime(db.getEndTime());

        assertThatThrownBy(() -> service.updateEvent(id, req))
                .hasMessageContaining(TIME_RANGE_INVALID.getMsg());
    }

    @Test
    void getByOrganizerId_missingOrganizer_throws() {
        when(userMapper.selectById(999L)).thenReturn(null);

        assertThrowsCode(() -> service.getByOrganizerId(999L), ORGANIZER_NOT_FOUND);
    }

    @Test
    void getByOrganizerId_noEvents_returnsEmptyList() {
        long organizerId = 888L;
        when(userMapper.selectById(organizerId))
                .thenReturn(UserDO.builder().id(organizerId).build());
        when(eventMapper.selectList(any())).thenReturn(List.of());
        when(userGroupMapper.selectList(any())).thenReturn(List.of());

        assertThat(service.getByOrganizerId(organizerId)).isEmpty();
    }

    @Test
    void getByOrganizerId_withEvents_populatesAggregates() {
        long organizerId = 777L;
        when(userMapper.selectById(organizerId))
                .thenReturn(UserDO.builder().id(organizerId).build());
        EventDO e1 = persistedEvent(101L).toBuilder().userId(organizerId).build();
        e1.setCreateTime(LocalDateTime.of(2025, 1, 1, 10, 0));
        EventDO e2 = persistedEvent(202L).toBuilder().userId(organizerId).build();
        e2.setCreateTime(LocalDateTime.of(2025, 1, 1, 12, 0));
        when(eventMapper.selectList(any())).thenReturn(List.of(e1, e2));
        when(userGroupMapper.selectList(any()))
                .thenReturn(
                        List.of(
                                UserGroupDO.builder().eventId(101L).userId(1L).build(),
                                UserGroupDO.builder().eventId(101L).userId(2L).build(),
                                UserGroupDO.builder().eventId(202L).userId(3L).build()));
        when(deptMapper.selectList(any()))
                .thenReturn(
                        List.of(
                                DeptDO.builder()
                                        .id(500L)
                                        .eventId(101L)
                                        .name("Group-101")
                                        .status(CommonStatusEnum.ENABLE.getStatus())
                                        .build(),
                                DeptDO.builder()
                                        .id(600L)
                                        .eventId(202L)
                                        .name("Group-202")
                                        .status(CommonStatusEnum.ENABLE.getStatus())
                                        .build()));
        when(taskMapper.selectList(any()))
                .thenReturn(
                        List.of(
                                TaskDO.builder()
                                        .eventId(101L)
                                        .status(TaskStatusEnum.COMPLETED.getStatus())
                                        .build(),
                                TaskDO.builder()
                                        .eventId(101L)
                                        .status(TaskStatusEnum.PROGRESS.getStatus())
                                        .build(),
                                TaskDO.builder()
                                        .eventId(202L)
                                        .status(TaskStatusEnum.PROGRESS.getStatus())
                                        .build()));

        List<EventRespVO> resp = service.getByOrganizerId(organizerId);

        assertThat(resp).hasSize(2);
        assertThat(resp).extracting(EventRespVO::getId).containsExactly(202L, 101L);
        EventRespVO first =
                resp.stream().filter(vo -> vo.getId().equals(101L)).findFirst().orElseThrow();
        assertThat(first.getJoiningParticipants()).isEqualTo(2);
        assertThat(first.getGroups())
                .extracting(EventRespVO.GroupVO::getName)
                .containsExactly("Group-101");
        assertThat(first.getTaskStatus().getTotal()).isEqualTo(2);
        assertThat(first.getTaskStatus().getCompleted()).isEqualTo(1);
        assertThat(first.getTaskStatus().getRemaining()).isEqualTo(1);

        EventRespVO second =
                resp.stream().filter(vo -> vo.getId().equals(202L)).findFirst().orElseThrow();
        assertThat(second.getJoiningParticipants()).isEqualTo(1);
        assertThat(second.getGroups())
                .extracting(EventRespVO.GroupVO::getName)
                .containsExactly("Group-202");
        assertThat(second.getTaskStatus().getTotal()).isEqualTo(1);
        assertThat(second.getTaskStatus().getCompleted()).isZero();
        assertThat(second.getTaskStatus().getRemaining()).isEqualTo(1);
    }

    @Test
    void getByOrganizerId_participantOnly_returnsEvents() {
        long userId = 555L;
        when(userMapper.selectById(userId)).thenReturn(UserDO.builder().id(userId).build());
        when(eventMapper.selectList(any())).thenReturn(List.of());
        when(userGroupMapper.selectList(any()))
                .thenReturn(List.of(UserGroupDO.builder().userId(userId).eventId(303L).build()));
        EventDO participantEvent = persistedEvent(303L).toBuilder().userId(999L).build();
        participantEvent.setCreateTime(LocalDateTime.of(2025, 1, 2, 0, 0));
        when(eventMapper.selectBatchIds(anyCollection())).thenReturn(List.of(participantEvent));
        when(deptMapper.selectList(any())).thenReturn(List.of());
        when(taskMapper.selectList(any())).thenReturn(List.of());

        List<EventRespVO> events = service.getByOrganizerId(userId);

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getId()).isEqualTo(303L);
    }

    @Test
    void getByOrganizerId_mixedOrganizerAndParticipant_sortedByCreateTime() {
        long userId = 321L;
        when(userMapper.selectById(userId)).thenReturn(UserDO.builder().id(userId).build());

        EventDO ownedLater = persistedEvent(400L).toBuilder().userId(userId).build();
        ownedLater.setCreateTime(LocalDateTime.of(2025, 2, 1, 0, 0));
        when(eventMapper.selectList(any())).thenReturn(List.of(ownedLater));

        when(userGroupMapper.selectList(any()))
                .thenReturn(List.of(UserGroupDO.builder().userId(userId).eventId(50L).build()));

        EventDO joinedEarlier = persistedEvent(50L).toBuilder().userId(999L).build();
        joinedEarlier.setCreateTime(LocalDateTime.of(2024, 12, 31, 0, 0));
        when(eventMapper.selectBatchIds(anyCollection())).thenReturn(List.of(joinedEarlier));

        when(deptMapper.selectList(any())).thenReturn(List.of());
        when(taskMapper.selectList(any())).thenReturn(List.of());

        List<EventRespVO> events = service.getByOrganizerId(userId);

        assertThat(events).extracting(EventRespVO::getId).containsExactly(400L, 50L);
    }

    @Test
    void deleteEvent_success() {
        when(eventMapper.selectById(10L)).thenReturn(persistedEvent(10L));
        when(eventMapper.deleteById(10L)).thenReturn(1);
        when(userGroupMapper.delete(any())).thenReturn(2);

        boolean ok = service.deleteEvent(10L);

        assertThat(ok).isTrue();
        verify(userGroupMapper).delete(any());
    }

    @Test
    void restoreEvent_success() {
        EventDO deletedEvent = persistedEvent(5L);
        deletedEvent.setDeleted(true);

        when(eventMapper.selectRawById(5L)).thenReturn(deletedEvent);
        when(eventMapper.restoreById(5L)).thenReturn(1);
        when(userGroupMapper.restoreByEventId(5L)).thenReturn(1);

        boolean ok = service.restoreEvent(5L);

        assertThat(ok).isTrue();
        verify(eventMapper).restoreById(5L);
        verify(userGroupMapper).restoreByEventId(5L);
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
