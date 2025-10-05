package nus.edu.u.system.service.Event;

import static nus.edu.u.system.enums.ErrorCodeConstants.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.common.enums.EventStatusEnum;
import nus.edu.u.common.exception.ErrorCode;
import nus.edu.u.common.exception.ServiceException;
import nus.edu.u.system.domain.dataobject.dept.DeptDO;
import nus.edu.u.system.domain.dataobject.task.EventDO;
import nus.edu.u.system.domain.dataobject.task.EventParticipantDO;
import nus.edu.u.system.domain.dataobject.task.TaskDO;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.vo.event.*;
import nus.edu.u.system.domain.vo.group.GroupRespVO;
import nus.edu.u.system.enums.ErrorCodeConstants;
import nus.edu.u.system.enums.task.TaskStatusEnum;
import nus.edu.u.system.mapper.dept.DeptMapper;
import nus.edu.u.system.mapper.task.EventMapper;
import nus.edu.u.system.mapper.task.EventParticipantMapper;
import nus.edu.u.system.mapper.task.TaskMapper;
import nus.edu.u.system.mapper.user.UserMapper;
import nus.edu.u.system.service.event.EventServiceImpl;
import nus.edu.u.system.service.group.GroupService;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock private EventMapper eventMapper;
    @Mock private EventParticipantMapper eventParticipantMapper;
    @Mock private UserMapper userMapper;
    @Mock private DeptMapper deptMapper;
    @Mock private TaskMapper taskMapper;
    @Mock private GroupService groupService;

    @InjectMocks private EventServiceImpl service;

    // ---------- helpers ----------
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
        return EventDO.builder()
                .id(id)
                .userId(111L)
                .name("NUS Hackathon")
                .description("Annual hackathon for students")
                .location("UTown Hall")
                .startTime(LocalDateTime.of(2025, 11, 15, 9, 0))
                .endTime(LocalDateTime.of(2025, 11, 15, 17, 0))
                .status(EventStatusEnum.ACTIVE.getCode())
                .build();
    }

    // ---------- tests ----------
    @Test
    void createEvent_success() {
        // 组织者、参与者存在
        when(userMapper.selectById(111L)).thenReturn(UserDO.builder().id(111L).build());
        when(userMapper.selectBatchIds(List.of(201L, 202L)))
                .thenReturn(
                        List.of(
                                UserDO.builder().id(201L).build(),
                                UserDO.builder().id(202L).build()));

        // insert 时通常由 MyBatis-Plus 赋 ID。这里模拟：insert 后，我们再去 selectById 返回持久化后的对象
        // 你也可以在 doAnswer 里把 event.setId(xxx)；这里走简单路径：调用结束后我们用 mapper.count 计算参与者数返回
        doAnswer(
                        inv -> {
                            // 写入成功
                            return 1;
                        })
                .when(eventMapper)
                .insert(any(EventDO.class));

        when(eventParticipantMapper.insert(any(EventParticipantDO.class))).thenReturn(1);

        when(eventParticipantMapper.selectCount(any())).thenReturn(2L);

        // 调用
        EventRespVO resp = service.createEvent(newCreateReq());

        // 断言（name/location 会从 DO->VO 转换，你的 MapStruct 已映射）
        assertThat(resp.getName()).isEqualTo("NUS Hackathon");
        assertThat(resp.getLocation()).isEqualTo("UTown Hall");
        assertThat(resp.getJoiningParticipants()).isEqualTo(2);
        // 状态默认 ACTIVE
        assertThat(resp.getStatus()).isEqualTo(EventStatusEnum.ACTIVE.getCode());

        // 交互验证
        verify(eventMapper, times(1)).insert(any(EventDO.class));
        verify(eventParticipantMapper, times(2)).insert(any(EventParticipantDO.class));
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
    void getByEventId_missing_throws() {
        when(eventMapper.selectById(1L)).thenReturn(null);

        assertThrowsCode(() -> service.getByEventId(1L), EVENT_NOT_FOUND);
    }

    @Test
    void getByEventId_success_populatesAggregates() {
        long eventId = 42L;
        EventDO event = persistedEvent(eventId);
        when(eventMapper.selectById(eventId)).thenReturn(event);
        when(eventParticipantMapper.selectCount(any())).thenReturn(3L);
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
                .satisfies(
                        group -> {
                            assertThat(group.getId()).isEqualTo("10");
                            assertThat(group.getName()).isEqualTo("Alpha");
                        });
        assertThat(resp.getTaskStatus().getTotal()).isEqualTo(2);
        assertThat(resp.getTaskStatus().getCompleted()).isEqualTo(1);
        assertThat(resp.getTaskStatus().getRemaining()).isEqualTo(1);
    }

    @Test
    void getByEventId_success_withoutTasksUsesEmptyStatus() {
        long eventId = 43L;
        when(eventMapper.selectById(eventId)).thenReturn(persistedEvent(eventId));
        when(eventParticipantMapper.selectCount(any())).thenReturn(0L);
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
    void updateEvent_success_patchAndSyncParticipants() {
        Long id = 7L;

        when(eventMapper.selectById(id)).thenReturn(persistedEvent(id));

        when(userMapper.selectById(222L)).thenReturn(UserDO.builder().id(222L).build());
        when(userMapper.selectBatchIds(List.of(301L, 303L)))
                .thenReturn(
                        List.of(
                                UserDO.builder().id(301L).build(),
                                UserDO.builder().id(303L).build()));

        // 关键：只保留这条连续 thenReturn
        when(eventParticipantMapper.selectList(any()))
                .thenReturn(
                        List.of( // 第一次：当前存在 301,302
                                ep(id, 301L), ep(id, 302L)))
                .thenReturn(
                        List.of( // 第二次：更新后 301,303
                                ep(id, 301L), ep(id, 303L)));

        when(eventMapper.updateById(any(EventDO.class))).thenReturn(1);
        when(eventParticipantMapper.delete(any())).thenReturn(1); // 删除 302
        when(eventParticipantMapper.insert(any(EventParticipantDO.class))).thenReturn(1); // 新增 303
        when(eventMapper.selectById(id))
                .thenReturn(persistedEvent(id).toBuilder().userId(222L).remark("patched").build());

        EventUpdateReqVO req = new EventUpdateReqVO();
        req.setOrganizerId(222L);
        req.setRemark("patched");
        req.setParticipantUserIds(List.of(301L, 303L));

        UpdateEventRespVO resp = service.updateEvent(id, req);

        assertThat(resp.getOrganizerId()).isEqualTo(222L);
        assertThat(resp.getRemarks()).isEqualTo("patched");
        assertThat(resp.getParticipantUserIds()).containsExactlyInAnyOrder(301L, 303L);

        verify(eventParticipantMapper, times(1)).delete(any()); // 删 302
        verify(eventParticipantMapper, times(1)).insert(any(EventParticipantDO.class)); // 加 303
    }

    private static EventParticipantDO ep(Long eventId, Long userId) {
        return EventParticipantDO.builder().eventId(eventId).userId(userId).build();
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

        assertThat(service.getByOrganizerId(organizerId)).isEmpty();
    }

    @Test
    void getByOrganizerId_withEvents_populatesAggregates() {
        long organizerId = 777L;
        when(userMapper.selectById(organizerId))
                .thenReturn(UserDO.builder().id(organizerId).build());
        EventDO e1 = persistedEvent(101L).toBuilder().userId(organizerId).build();
        EventDO e2 = persistedEvent(202L).toBuilder().userId(organizerId).build();
        when(eventMapper.selectList(any())).thenReturn(List.of(e1, e2));
        when(eventParticipantMapper.selectList(any()))
                .thenReturn(
                        List.of(
                                EventParticipantDO.builder().eventId(101L).userId(1L).build(),
                                EventParticipantDO.builder().eventId(101L).userId(2L).build(),
                                EventParticipantDO.builder().eventId(202L).userId(3L).build()));
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
    void deleteEvent_success() {
        when(eventMapper.selectById(10L)).thenReturn(persistedEvent(10L));
        when(eventMapper.deleteById(10L)).thenReturn(1);
        when(eventParticipantMapper.delete(any())).thenReturn(2);

        boolean ok = service.deleteEvent(10L);
        assertThat(ok).isTrue();

        verify(eventParticipantMapper).delete(any());
    }

    @Test
    void restoreEvent_success() {
        EventDO deletedEvent = persistedEvent(5L);
        deletedEvent.setDeleted(true);

        when(eventMapper.selectRawById(5L)).thenReturn(deletedEvent);
        when(eventMapper.restoreById(5L)).thenReturn(1);
        when(eventParticipantMapper.restoreByEventId(5L)).thenReturn(1);

        boolean ok = service.restoreEvent(5L);
        assertThat(ok).isTrue();

        verify(eventMapper).restoreById(5L);
        verify(eventParticipantMapper).restoreByEventId(5L);
    }

    // @Test
    // void getByOrganizerId_organizerNotFound_throws() {
    //     when(userMapper.selectById(999L)).thenReturn(null);

    //     assertThatThrownBy(() -> service.getByOrganizerId(999L))
    //             .hasMessageContaining(ORGANIZER_NOT_FOUND.getMsg());
    // }

    // // 2) getByOrganizerId — 无活动
    // @Test
    // void getByOrganizerId_noEvents_returnsEmpty() {
    //     when(userMapper.selectById(111L)).thenReturn(UserDO.builder().id(111L).build());
    //     when(eventMapper.selectList(any())).thenReturn(List.of());

    //     List<EventRespVO> list = service.getByOrganizerId(111L);
    //     assertThat(list).isEmpty();
    // }

    // // 3) getByOrganizerId — 有活动，校验计数与扩展字段
    // @Test
    // void getByOrganizerId_hasEvents_returnsWithCountsAndExtras() {
    //     Long organizerId = 111L;
    //     when(userMapper.selectById(organizerId))
    //             .thenReturn(UserDO.builder().id(organizerId).build());

    //     EventDO e1 = persistedEvent(1L).toBuilder().userId(organizerId).build();
    //     EventDO e2 = persistedEvent(2L).toBuilder().userId(organizerId).build();
    //     when(eventMapper.selectList(any())).thenReturn(List.of(e1, e2));

    //     // e1 有两人，e2 有一人
    //     when(eventParticipantMapper.selectList(any()))
    //             .thenReturn(
    //                     List.of(
    //                             EventParticipantDO.builder().eventId(1L).userId(301L).build(),
    //                             EventParticipantDO.builder().eventId(1L).userId(302L).build(),
    //                             EventParticipantDO.builder().eventId(2L).userId(303L).build()));

    //     List<EventRespVO> list = service.getByOrganizerId(organizerId);

    //     assertThat(list).hasSize(2);
    //     assertThat(
    //                     list.stream()
    //                             .filter(v -> v.getId().equals(1L))
    //                             .findFirst()
    //                             .orElseThrow()
    //                             .getJoiningParticipants())
    //             .isEqualTo(2);
    //     assertThat(
    //                     list.stream()
    //                             .filter(v -> v.getId().equals(2L))
    //                             .findFirst()
    //                             .orElseThrow()
    //                             .getJoiningParticipants())
    //             .isEqualTo(1);

    //     // groups / taskStatus 是硬编码
    //     EventRespVO any = list.get(0);
    //     assertThat(any.getGroups()).extracting("id").containsExactly("grp_a", "grp_b");
    //     assertThat(any.getTaskStatus().getTotal()).isEqualTo(0);
    // }

    // 4) createEvent — 时间非法触发 TIME_RANGE_INVALID
    @Test
    void createEvent_timeInvalid_throws() {
        EventCreateReqVO req = new EventCreateReqVO();
        req.setOrganizerId(111L);
        req.setStartTime(LocalDateTime.of(2025, 1, 1, 10, 0));
        req.setEndTime(LocalDateTime.of(2025, 1, 1, 10, 0)); // start >= end

        // 不要 stub userMapper.selectById(111L)

        assertThatThrownBy(() -> service.createEvent(req))
                .hasMessageContaining(TIME_RANGE_INVALID.getMsg());
    }

    // 5) updateEvent — 时间非法（也可测 create 的就够）
    @Test
    void updateEvent_timeInvalid_throws() {
        Long id = 7L;
        EventDO db = persistedEvent(id);
        when(eventMapper.selectById(id)).thenReturn(db);

        EventUpdateReqVO req = new EventUpdateReqVO();
        req.setStartTime(db.getEndTime()); // 让 start >= end
        // 不改 end -> 使用 db 的 end

        assertThatThrownBy(() -> service.updateEvent(id, req))
                .hasMessageContaining(TIME_RANGE_INVALID.getMsg());
    }

    // 6) createEvent — 参与者重复
    @Test
    void createEvent_participantsDuplicate_throws() {
        EventCreateReqVO req = new EventCreateReqVO();
        req.setOrganizerId(111L);
        req.setStartTime(LocalDateTime.now());
        req.setEndTime(LocalDateTime.now().plusHours(1));
        req.setParticipantUserIds(List.of(301L, 301L)); // 重复
        when(userMapper.selectById(111L)).thenReturn(UserDO.builder().id(111L).build());

        assertThatThrownBy(() -> service.createEvent(req))
                .hasMessageContaining(DUPLICATE_PARTICIPANTS.getMsg());
    }

    // 7) createEvent — 参与者缺失
    @Test
    void createEvent_participantsMissing_throws() {
        EventCreateReqVO req = new EventCreateReqVO();
        req.setOrganizerId(111L);
        req.setStartTime(LocalDateTime.now());
        req.setEndTime(LocalDateTime.now().plusHours(1));
        req.setParticipantUserIds(List.of(301L, 999L)); // 999 不存在

        when(userMapper.selectById(111L)).thenReturn(UserDO.builder().id(111L).build());
        when(userMapper.selectBatchIds(List.of(301L, 999L)))
                .thenReturn(List.of(UserDO.builder().id(301L).build())); // 少一个

        assertThatThrownBy(() -> service.createEvent(req))
                .hasMessageContaining(PARTICIPANT_NOT_FOUND.getMsg());
    }

    // 8) deleteEvent — deleteById 返回 0 触发 EVENT_DELETE_FAILED
    @Test
    void deleteEvent_deleteFailed_throws() {
        Long id = 5L;
        when(eventMapper.selectById(id)).thenReturn(persistedEvent(id));
        when(eventMapper.deleteById(id)).thenReturn(0);

        assertThatThrownBy(() -> service.deleteEvent(id))
                .hasMessageContaining(EVENT_DELETE_FAILED.getMsg());
    }

    // 9) restoreEvent — raw 查无此对象
    @Test
    void restoreEvent_notFound_throws() {
        when(eventMapper.selectRawById(5L)).thenReturn(null);

        assertThatThrownBy(() -> service.restoreEvent(5L))
                .hasMessageContaining(EVENT_NOT_FOUND.getMsg());
    }

    // 10) restoreEvent — deleted=false 触发 EVENT_NOT_DELETED
    @Test
    void restoreEvent_notDeleted_throws() {
        EventDO raw = persistedEvent(5L);
        raw.setDeleted(false); // 关键：显式设为 false

        when(eventMapper.selectRawById(5L)).thenReturn(raw);

        assertThatThrownBy(() -> service.restoreEvent(5L))
                .hasMessageContaining(EVENT_NOT_DELETED.getMsg());
    }

    @Test
    void restoreEvent_restoreFailed_throws() {
        EventDO raw = persistedEvent(5L);
        raw.setDeleted(true);
        when(eventMapper.selectRawById(5L)).thenReturn(raw);
        when(eventMapper.restoreById(5L)).thenReturn(0);

        assertThatThrownBy(() -> service.restoreEvent(5L))
                .hasMessageContaining(EVENT_RESTORE_FAILED.getMsg());
    }

    @Test
    void updateEvent_timeRange_ok_whenEndNull() {
        when(eventMapper.selectById(7L)).thenReturn(persistedEvent(7L)); // db里有end
        EventUpdateReqVO req = new EventUpdateReqVO();
        req.setStartTime(persistedEvent(7L).getStartTime()); // 给start, end留null
        when(eventMapper.updateById(any())).thenReturn(1);
        when(eventMapper.selectById(7L)).thenReturn(persistedEvent(7L));
        UpdateEventRespVO resp = service.updateEvent(7L, req);
        assertThat(resp.getId()).isEqualTo(7L);
    }

    @Test
    void updateEvent_timeRange_invalid() {
        when(eventMapper.selectById(7L)).thenReturn(persistedEvent(7L));
        EventUpdateReqVO req = new EventUpdateReqVO();
        req.setStartTime(LocalDateTime.of(2025, 10, 1, 10, 0));
        req.setEndTime(LocalDateTime.of(2025, 10, 1, 9, 0)); // start>=end

        assertThrowsCode(() -> service.updateEvent(7L, req), TIME_RANGE_INVALID);
    }

    @Test
    void createEvent_noOrganizer_throws() {
        EventCreateReqVO req = baseCreate();
        req.setOrganizerId(null);
        assertThrowsCode(() -> service.createEvent(req), ORGANIZER_NOT_FOUND);
    }

    @Test
    void createEvent_organizerNotFound_throws() {
        EventCreateReqVO req = baseCreate();
        req.setOrganizerId(111L);
        when(userMapper.selectById(111L)).thenReturn(null);
        assertThrowsCode(() -> service.createEvent(req), ORGANIZER_NOT_FOUND);
    }

    @Test
    void createEvent_organizerOk() {
        EventCreateReqVO req = baseCreate();
        req.setOrganizerId(111L);
        when(userMapper.selectById(111L)).thenReturn(UserDO.builder().id(111L).build());
        when(eventMapper.insert(any())).thenReturn(1);
        when(eventParticipantMapper.selectCount(any())).thenReturn(0L);
        EventRespVO resp = service.createEvent(req);
        assertThat(resp.getOrganizerId()).isEqualTo(111L);
    }

    @Test
    void createEvent_participants_null_earlyReturn() {
        EventCreateReqVO req = baseCreate();
        req.setParticipantUserIds(null);
        okOrganizer(req.getOrganizerId());
        when(eventMapper.insert(any())).thenReturn(1);
        when(eventParticipantMapper.selectCount(any())).thenReturn(0L);
        service.createEvent(req);
        verify(userMapper, never()).selectBatchIds(any());
    }

    @Test
    void createEvent_participants_empty_earlyReturn() {
        EventCreateReqVO req = baseCreate();
        req.setParticipantUserIds(List.of());
        okOrganizer(req.getOrganizerId());
        when(eventMapper.insert(any())).thenReturn(1);
        when(eventParticipantMapper.selectCount(any())).thenReturn(0L);
        service.createEvent(req);
        verify(userMapper, never()).selectBatchIds(any());
    }

    @Test
    void createEvent_participants_duplicate_throws() {
        EventCreateReqVO req = baseCreate();
        req.setParticipantUserIds(List.of(1L, 1L));
        okOrganizer(req.getOrganizerId());
        assertThrowsCode(() -> service.createEvent(req), DUPLICATE_PARTICIPANTS);
    }

    @Test
    void createEvent_participants_missing_throws() {
        EventCreateReqVO req = baseCreate();
        req.setParticipantUserIds(List.of(1L, 999L));
        okOrganizer(req.getOrganizerId());
        when(userMapper.selectBatchIds(List.of(1L, 999L)))
                .thenReturn(List.of(UserDO.builder().id(1L).build())); // 999 缺失
        assertThrowsCode(() -> service.createEvent(req), PARTICIPANT_NOT_FOUND);
    }

    @Test
    void createEvent_participants_allExist_ok() {
        EventCreateReqVO req = baseCreate();
        req.setParticipantUserIds(List.of(1L, 2L));
        okOrganizer(req.getOrganizerId());
        when(userMapper.selectBatchIds(List.of(1L, 2L)))
                .thenReturn(
                        List.of(UserDO.builder().id(1L).build(), UserDO.builder().id(2L).build()));
        when(eventMapper.insert(any())).thenReturn(1);
        when(eventParticipantMapper.insert(any())).thenReturn(1);
        when(eventParticipantMapper.selectCount(any())).thenReturn(2L);

        EventRespVO resp = service.createEvent(req);
        assertThat(resp.getJoiningParticipants()).isEqualTo(2);
    }

    @Test
    void createEvent_status_null_defaultsToActive() {
        EventCreateReqVO req = baseCreate();
        req.setStatus(null);
        okOrganizer(req.getOrganizerId());
        when(eventMapper.insert(any())).thenReturn(1);
        when(eventParticipantMapper.selectCount(any())).thenReturn(0L);
        EventRespVO resp = service.createEvent(req);
        assertThat(resp.getStatus()).isEqualTo(EventStatusEnum.ACTIVE.getCode());
    }

    @Test
    void createEvent_status_given_kept() {
        EventCreateReqVO req = baseCreate();
        req.setStatus(EventStatusEnum.ACTIVE.getCode());
        okOrganizer(req.getOrganizerId());
        when(eventMapper.insert(any())).thenReturn(1);
        when(eventParticipantMapper.selectCount(any())).thenReturn(0L);
        EventRespVO resp = service.createEvent(req);
        assertThat(resp.getStatus()).isEqualTo(EventStatusEnum.ACTIVE.getCode());
    }

    // @Test
    // void getByOrganizerId_notFound_throws() {
    //     when(userMapper.selectById(888L)).thenReturn(null);
    //     assertThrowsCode(() -> service.getByOrganizerId(888L), ORGANIZER_NOT_FOUND);
    // }

    // @Test
    // void getByOrganizerId_ok() {
    //     when(userMapper.selectById(111L)).thenReturn(UserDO.builder().id(111L).build());
    //     when(eventMapper.selectList(any()))
    //             .thenReturn(
    //                     List.of(
    //                             persistedEvent(1L).toBuilder().userId(111L).build(),
    //                             persistedEvent(2L).toBuilder().userId(111L).build()));
    //     when(eventParticipantMapper.selectList(any())).thenReturn(List.of()); // 0人
    //     List<EventRespVO> list = service.getByOrganizerId(111L);
    //     assertThat(list).hasSize(2);
    //     assertThat(list.get(0).getJoiningParticipants()).isZero();
    //     assertThat(list.get(0).getGroups()).isNotEmpty();
    //     assertThat(list.get(0).getTaskStatus()).isNotNull();
    // }

    @Test
    void deleteEvent_ok() {
        when(eventMapper.selectById(9L)).thenReturn(persistedEvent(9L));
        when(eventMapper.deleteById(9L)).thenReturn(1);
        when(eventParticipantMapper.delete(any())).thenReturn(1);
        assertThat(service.deleteEvent(9L)).isTrue();
    }

    private EventCreateReqVO baseCreate() {
        EventCreateReqVO req = new EventCreateReqVO();
        req.setEventName("Test Event");
        req.setDescription("Test Desc");
        req.setOrganizerId(111L);
        req.setStartTime(LocalDateTime.of(2025, 10, 1, 9, 0));
        req.setEndTime(LocalDateTime.of(2025, 10, 1, 17, 0));
        req.setLocation("Test Location");
        return req;
    }

    private void okOrganizer(long id) {
        when(userMapper.selectById(id)).thenReturn(UserDO.builder().id(id).build());
    }

    private void assertThrowsCode(ThrowableAssert.ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call)
                .isInstanceOf(ServiceException.class)
                .satisfies(
                        ex -> {
                            ServiceException se = (ServiceException) ex;
                            // 主断言：code
                            org.assertj.core.api.Assertions.assertThat(se.getCode())
                                    .isEqualTo(code.getCode());
                            // 辅助断言（可选）：message 与常量文案一致
                            org.assertj.core.api.Assertions.assertThat(se.getMessage())
                                    .contains(code.getMsg()); // 或者 .isEqualTo(code.getMsg())
                        });
    }

    @Test
    void createEvent_startNull_endOk_ok() {
        EventCreateReqVO req = new EventCreateReqVO();
        req.setOrganizerId(111L);
        req.setStartTime(null);
        req.setEndTime(LocalDateTime.now().plusHours(1));

        when(userMapper.selectById(111L)).thenReturn(UserDO.builder().id(111L).build());
        when(eventMapper.insert(any())).thenReturn(1);

        // 不抛异常即可
        service.createEvent(req);
    }

    @Test
    void createEvent_validTime_ok() {
        EventCreateReqVO req = new EventCreateReqVO();
        req.setOrganizerId(111L);
        req.setStartTime(LocalDateTime.now().plusHours(1));
        req.setEndTime(LocalDateTime.now().plusHours(2));

        when(userMapper.selectById(111L)).thenReturn(UserDO.builder().id(111L).build());
        when(eventMapper.insert(any())).thenReturn(1);

        service.createEvent(req); // 不抛异常
    }

    @Test
    void createEvent_invalidTime_throws() {
        EventCreateReqVO req = new EventCreateReqVO();
        req.setOrganizerId(111L);
        req.setStartTime(LocalDateTime.now().plusHours(2));
        req.setEndTime(LocalDateTime.now().plusHours(1));

        assertThatThrownBy(() -> service.createEvent(req))
                .hasMessageContaining("The start time must be earlier than the end time");
    }

    @Test
    void createEvent_validTime_ok2() {
        // 准备一个合法时间
        var now = LocalDateTime.now();
        EventCreateReqVO req = new EventCreateReqVO();
        req.setOrganizerId(111L);
        req.setStartTime(now.plusHours(1));
        req.setEndTime(now.plusHours(2));
        req.setEventName("ok");
        req.setLocation("x");

        // 其他校验最小化通过
        when(userMapper.selectById(111L)).thenReturn(UserDO.builder().id(111L).build());
        when(eventMapper.insert(any(EventDO.class))).thenReturn(1);
        when(eventParticipantMapper.selectCount(any())).thenReturn(0L);

        // 不应抛异常（命中 validateTimeRange 的 false 分支）
        assertThatCode(() -> service.createEvent(req)).doesNotThrowAnyException();
    }

    @Test
    void validateTimeRange_trueBranch_throws() {
        LocalDateTime s = LocalDateTime.now().plusHours(2);
        LocalDateTime e = LocalDateTime.now().plusHours(1);
        assertThatThrownBy(
                        () -> ReflectionTestUtils.invokeMethod(service, "validateTimeRange", s, e))
                .hasMessageContaining("The start time must be earlier than the end time");
    }

    @Test
    void validateTimeRange_falseBranch_ok() {
        LocalDateTime s = LocalDateTime.now().plusHours(1);
        LocalDateTime e = LocalDateTime.now().plusHours(2);
        assertThatCode(() -> ReflectionTestUtils.invokeMethod(service, "validateTimeRange", s, e))
                .doesNotThrowAnyException();
    }

    @Test
    void validateTimeRange_startNull_ok() {
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        // start = null
        assertThatCode(
                        () ->
                                ReflectionTestUtils.invokeMethod(
                                        service, "validateTimeRange", null, end))
                .doesNotThrowAnyException();
    }

    @Test
    void validateTimeRange_endNull_ok() {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        // end = null
        assertThatCode(
                        () ->
                                ReflectionTestUtils.invokeMethod(
                                        service, "validateTimeRange", start, null))
                .doesNotThrowAnyException();
    }

    @Test
    void deleteEvent_notFound_throws() {
        // arrange
        Long eventId = 123L;
        when(eventMapper.selectById(eventId)).thenReturn(null);

        // act & assert
        assertThatThrownBy(() -> service.deleteEvent(eventId))
                .hasMessageContaining("Event not found");
    }

    @Test
    void updateEvent_notFound_throws() {
        // arrange
        Long eventId = 123L;
        EventUpdateReqVO reqVO = new EventUpdateReqVO();
        when(eventMapper.selectById(eventId)).thenReturn(null);

        // act & assert
        assertThatThrownBy(() -> service.updateEvent(eventId, reqVO))
                .hasMessageContaining("Event not found");
    }

    @Test
    void updateEvent_skipOrganizerValidation_whenOrganizerIdNull() {
        long id = 1L;
        // DB中原始记录
        when(eventMapper.selectById(id)).thenReturn(persistedEvent(id));

        // participants 为 null，避免走后面的分支
        EventUpdateReqVO req = new EventUpdateReqVO();
        req.setOrganizerId(null);
        req.setRemark("only patch");

        when(eventMapper.updateById(any(EventDO.class))).thenReturn(1);
        when(eventMapper.selectById(id))
                .thenReturn(persistedEvent(id).toBuilder().remark("only patch").build());
        when(eventParticipantMapper.selectList(any())).thenReturn(List.of()); // 用于最后组装resp

        UpdateEventRespVO resp = service.updateEvent(id, req);

        assertThat(resp.getRemarks()).isEqualTo("only patch");
        // 关键：没查 organizer
        verify(userMapper, never()).selectById(anyLong());
    }

    @Test
    void updateEvent_sync_onlyRemove() {
        long id = 3L;
        when(eventMapper.selectById(id)).thenReturn(persistedEvent(id));
        // 当前 301,302（第一次 selectList，用于差异计算）
        when(eventParticipantMapper.selectList(any()))
                .thenReturn(
                        List.of(
                                EventParticipantDO.builder().eventId(id).userId(301L).build(),
                                EventParticipantDO.builder().eventId(id).userId(302L).build()))
                // 第二次：组装 resp
                .thenReturn(List.of(EventParticipantDO.builder().eventId(id).userId(301L).build()));

        EventUpdateReqVO req = new EventUpdateReqVO();
        req.setParticipantUserIds(List.of(301L));
        // 校验参与者存在
        when(userMapper.selectBatchIds(List.of(301L)))
                .thenReturn(List.of(UserDO.builder().id(301L).build()));

        when(eventMapper.updateById(any(EventDO.class))).thenReturn(1);
        when(eventMapper.selectById(id)).thenReturn(persistedEvent(id));

        UpdateEventRespVO resp = service.updateEvent(id, req);

        verify(eventParticipantMapper, times(1)).delete(any());
        verify(eventParticipantMapper, never()).insert(any());
        assertThat(resp.getParticipantUserIds()).containsExactly(301L);
    }

    @Test
    void updateEvent_sync_onlyAdd() {
        long id = 4L;
        when(eventMapper.selectById(id)).thenReturn(persistedEvent(id));
        when(eventParticipantMapper.selectList(any()))
                .thenReturn(List.of(EventParticipantDO.builder().eventId(id).userId(301L).build()))
                .thenReturn(
                        List.of(
                                EventParticipantDO.builder().eventId(id).userId(301L).build(),
                                EventParticipantDO.builder().eventId(id).userId(303L).build()));

        EventUpdateReqVO req = new EventUpdateReqVO();
        req.setParticipantUserIds(List.of(301L, 303L));

        when(userMapper.selectBatchIds(List.of(301L, 303L)))
                .thenReturn(
                        List.of(
                                UserDO.builder().id(301L).build(),
                                UserDO.builder().id(303L).build()));

        when(eventMapper.updateById(any(EventDO.class))).thenReturn(1);
        when(eventMapper.selectById(id)).thenReturn(persistedEvent(id));

        UpdateEventRespVO resp = service.updateEvent(id, req);

        verify(eventParticipantMapper, never()).delete(any());
        verify(eventParticipantMapper, times(1)).insert(any(EventParticipantDO.class));
        assertThat(resp.getParticipantUserIds()).containsExactlyInAnyOrder(301L, 303L);
    }

    @Test
    void updateEvent_sync_noChange() {
        long id = 5L;
        when(eventMapper.selectById(id)).thenReturn(persistedEvent(id));
        when(eventParticipantMapper.selectList(any()))
                .thenReturn(
                        List.of(
                                EventParticipantDO.builder().eventId(id).userId(301L).build(),
                                EventParticipantDO.builder().eventId(id).userId(302L).build()))
                .thenReturn(
                        List.of(
                                EventParticipantDO.builder().eventId(id).userId(301L).build(),
                                EventParticipantDO.builder().eventId(id).userId(302L).build()));

        EventUpdateReqVO req = new EventUpdateReqVO();
        req.setParticipantUserIds(List.of(301L, 302L));

        when(userMapper.selectBatchIds(List.of(301L, 302L)))
                .thenReturn(
                        List.of(
                                UserDO.builder().id(301L).build(),
                                UserDO.builder().id(302L).build()));

        when(eventMapper.updateById(any(EventDO.class))).thenReturn(1);
        when(eventMapper.selectById(id)).thenReturn(persistedEvent(id));

        UpdateEventRespVO resp = service.updateEvent(id, req);

        verify(eventParticipantMapper, never()).delete(any());
        verify(eventParticipantMapper, never()).insert(any());
        assertThat(resp.getParticipantUserIds()).containsExactlyInAnyOrder(301L, 302L);
    }

    @Test
    void assignableMember_groupListNull_returnsEmpty() {
        Long eventId = 100L;
        when(deptMapper.selectList(any())).thenReturn(null);

        List<EventGroupRespVO> result = service.assignableMember(eventId);

        assertThat(result).isEmpty();
        verify(groupService, never()).getGroupMembers(anyLong());
    }

    @Test
    void assignableMember_groupListEmpty_returnsEmpty() {
        Long eventId = 100L;
        when(deptMapper.selectList(any())).thenReturn(List.of());

        List<EventGroupRespVO> result = service.assignableMember(eventId);

        assertThat(result).isEmpty();
        verify(groupService, never()).getGroupMembers(anyLong());
    }

    @Test
    void assignableMember_singleGroupWithMembers_success() {
        Long eventId = 100L;
        Long groupId = 10L;

        DeptDO group = DeptDO.builder().id(groupId).eventId(eventId).name("Team Alpha").build();

        when(deptMapper.selectList(any())).thenReturn(List.of(group));

        GroupRespVO.MemberInfo member1 = new GroupRespVO.MemberInfo();
        member1.setUserId(201L);
        member1.setUsername("Alice");

        GroupRespVO.MemberInfo member2 = new GroupRespVO.MemberInfo();
        member2.setUserId(202L);
        member2.setUsername("Bob");

        when(groupService.getGroupMembers(groupId)).thenReturn(List.of(member1, member2));

        List<EventGroupRespVO> result = service.assignableMember(eventId);

        assertThat(result).hasSize(1);
        EventGroupRespVO groupVO = result.get(0);
        assertThat(groupVO.getId()).isEqualTo(groupId);
        assertThat(groupVO.getName()).isEqualTo("Team Alpha");
        assertThat(groupVO.getMembers()).hasSize(2);
        assertThat(groupVO.getMembers())
                .extracting(EventGroupRespVO.Member::getId)
                .containsExactly(201L, 202L);
        assertThat(groupVO.getMembers())
                .extracting(EventGroupRespVO.Member::getUsername)
                .containsExactly("Alice", "Bob");
    }

    @Test
    void assignableMember_multipleGroupsWithMembers_success() {
        Long eventId = 100L;

        DeptDO group1 = DeptDO.builder().id(10L).eventId(eventId).name("Team Alpha").build();

        DeptDO group2 = DeptDO.builder().id(20L).eventId(eventId).name("Team Beta").build();

        when(deptMapper.selectList(any())).thenReturn(List.of(group1, group2));

        GroupRespVO.MemberInfo member1 = new GroupRespVO.MemberInfo();
        member1.setUserId(201L);
        member1.setUsername("Alice");

        GroupRespVO.MemberInfo member2 = new GroupRespVO.MemberInfo();
        member2.setUserId(301L);
        member2.setUsername("Charlie");

        GroupRespVO.MemberInfo member3 = new GroupRespVO.MemberInfo();
        member3.setUserId(302L);
        member3.setUsername("David");

        when(groupService.getGroupMembers(10L)).thenReturn(List.of(member1));
        when(groupService.getGroupMembers(20L)).thenReturn(List.of(member2, member3));

        List<EventGroupRespVO> result = service.assignableMember(eventId);

        assertThat(result).hasSize(2);

        EventGroupRespVO alpha =
                result.stream().filter(g -> g.getId().equals(10L)).findFirst().orElseThrow();
        assertThat(alpha.getName()).isEqualTo("Team Alpha");
        assertThat(alpha.getMembers()).hasSize(1);
        assertThat(alpha.getMembers().get(0).getId()).isEqualTo(201L);

        EventGroupRespVO beta =
                result.stream().filter(g -> g.getId().equals(20L)).findFirst().orElseThrow();
        assertThat(beta.getName()).isEqualTo("Team Beta");
        assertThat(beta.getMembers()).hasSize(2);
        assertThat(beta.getMembers())
                .extracting(EventGroupRespVO.Member::getId)
                .containsExactly(301L, 302L);
    }

    @Test
    void assignableMember_groupWithNoMembers_returnsGroupWithEmptyMemberList() {
        Long eventId = 100L;
        Long groupId = 10L;

        DeptDO group = DeptDO.builder().id(groupId).eventId(eventId).name("Empty Team").build();

        when(deptMapper.selectList(any())).thenReturn(List.of(group));
        when(groupService.getGroupMembers(groupId)).thenReturn(List.of());

        List<EventGroupRespVO> result = service.assignableMember(eventId);

        assertThat(result).hasSize(1);
        EventGroupRespVO groupVO = result.get(0);
        assertThat(groupVO.getId()).isEqualTo(groupId);
        assertThat(groupVO.getName()).isEqualTo("Empty Team");
        assertThat(groupVO.getMembers()).isEmpty();
    }
}
