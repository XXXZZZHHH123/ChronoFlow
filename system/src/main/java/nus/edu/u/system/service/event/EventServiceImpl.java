package nus.edu.u.system.service.event;

import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.*;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.system.convert.event.EventConvert;
import nus.edu.u.system.domain.dataobject.dept.DeptDO;
import nus.edu.u.system.domain.dataobject.task.EventDO;
import nus.edu.u.system.domain.dataobject.task.TaskDO;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.dataobject.user.UserGroupDO;
import nus.edu.u.system.domain.dto.EventDTO;
import nus.edu.u.system.domain.vo.event.*;
import nus.edu.u.system.domain.vo.group.GroupRespVO;
import nus.edu.u.system.enums.event.EventStatusEnum;
import nus.edu.u.system.enums.task.TaskStatusEnum;
import nus.edu.u.system.mapper.dept.DeptMapper;
import nus.edu.u.system.mapper.task.EventMapper;
import nus.edu.u.system.mapper.task.TaskMapper;
import nus.edu.u.system.mapper.user.UserGroupMapper;
import nus.edu.u.system.mapper.user.UserMapper;
import nus.edu.u.system.service.event.validation.EventValidationContext;
import nus.edu.u.system.service.event.validation.EventValidationHandler;
import nus.edu.u.system.service.group.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class EventServiceImpl implements EventService {

    @Resource private GroupService groupService;

    @Resource private EventMapper eventMapper;

    @Resource private UserMapper userMapper;

    @Resource private DeptMapper deptMapper;

    @Resource private TaskMapper taskMapper;

    @Resource private UserGroupMapper userGroupMapper;

    @Autowired private List<EventValidationHandler> validationHandlers;

    @Override
    @Transactional
    public EventRespVO createEvent(EventCreateReqVO reqVO) {
        runValidations(EventValidationContext.forCreate(reqVO));

        EventDTO dto = EventConvert.INSTANCE.convert(reqVO);
        EventDO event = EventConvert.INSTANCE.convert(dto);
        if (event.getStatus() == null) {
            event.setStatus(EventStatusEnum.ACTIVE.getCode());
        }
        eventMapper.insert(event);

        EventRespVO resp = EventConvert.INSTANCE.DOconvertVO(event);

        Long eventId = event.getId();
        if (eventId != null) {
            Map<Long, Integer> participantCounts =
                    fetchParticipantCountsByEventIds(List.of(eventId));
            resp.setJoiningParticipants(participantCounts.getOrDefault(eventId, 0));
        } else {
            resp.setJoiningParticipants(0);
        }

        return resp;
    }

    @Override
    @Transactional(readOnly = true)
    public EventRespVO getByEventId(Long eventId) {
        EventDO event = eventMapper.selectById(eventId);
        if (event == null) {
            throw exception(EVENT_NOT_FOUND);
        }
        EventRespVO resp = EventConvert.INSTANCE.DOconvertVO(event);

        Map<Long, Integer> participantCounts = fetchParticipantCountsByEventIds(List.of(eventId));
        resp.setJoiningParticipants(participantCounts.getOrDefault(eventId, 0));

        resp.setGroups(fetchGroupsByEventIds(List.of(eventId)).getOrDefault(eventId, List.of()));
        resp.setTaskStatus(
                fetchTaskStatusesByEventIds(List.of(eventId))
                        .getOrDefault(eventId, emptyTaskStatus()));

        return resp;
    }

    @Override
    @Transactional
    public List<EventRespVO> getByOrganizerId(Long organizerId) {
        if (userMapper.selectById(organizerId) == null) {
            throw exception(ORGANIZER_NOT_FOUND);
        }

        List<EventDO> organizerEvents =
                eventMapper.selectList(
                        new LambdaQueryWrapper<EventDO>().eq(EventDO::getUserId, organizerId));

        List<UserGroupDO> memberships =
                userGroupMapper.selectList(
                        Wrappers.<UserGroupDO>lambdaQuery()
                                .eq(UserGroupDO::getUserId, organizerId));

        if ((organizerEvents == null || organizerEvents.isEmpty())
                && (memberships == null || memberships.isEmpty())) {
            return List.of();
        }

        Map<Long, EventDO> eventsById = new LinkedHashMap<>();
        if (organizerEvents != null) {
            organizerEvents.stream()
                    .filter(Objects::nonNull)
                    .forEach(event -> eventsById.put(event.getId(), event));
        }

        if (memberships != null && !memberships.isEmpty()) {
            Set<Long> participantEventIds =
                    memberships.stream()
                            .map(UserGroupDO::getEventId)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toCollection(LinkedHashSet::new));

            participantEventIds.removeAll(eventsById.keySet());

            if (!participantEventIds.isEmpty()) {
                List<EventDO> participantEvents = eventMapper.selectBatchIds(participantEventIds);
                if (participantEvents != null) {
                    participantEvents.stream()
                            .filter(Objects::nonNull)
                            .forEach(event -> eventsById.put(event.getId(), event));
                }
            }
        }

        if (eventsById.isEmpty()) {
            return List.of();
        }

        Comparator<EventDO> byCreateTimeDesc =
                Comparator.comparing(
                        (EventDO event) ->
                                Optional.ofNullable(event.getCreateTime())
                                        .orElse(LocalDateTime.MIN),
                        Comparator.reverseOrder());
        Comparator<EventDO> byIdDesc =
                Comparator.comparing(
                        (EventDO event) ->
                                Optional.ofNullable(event.getId()).orElse(Long.MIN_VALUE),
                        Comparator.reverseOrder());

        List<EventDO> orderedEvents =
                eventsById.values().stream()
                        .sorted(byCreateTimeDesc.thenComparing(byIdDesc))
                        .toList();

        List<Long> eventIds = orderedEvents.stream().map(EventDO::getId).toList();
        Map<Long, Integer> countsByEventId = fetchParticipantCountsByEventIds(eventIds);

        Map<Long, List<EventRespVO.GroupVO>> groupsByEventId = fetchGroupsByEventIds(eventIds);
        Map<Long, EventRespVO.TaskStatusVO> taskStatusByEventId =
                fetchTaskStatusesByEventIds(eventIds);

        // Active event
        for (EventDO event : orderedEvents) {
            if (ObjectUtil.isNull(event.getStartTime()) || ObjectUtil.isNull(event.getEndTime())) {
                continue;
            }
            if (LocalDateTime.now().isBefore(event.getStartTime())) {
                event.setStatus(EventStatusEnum.NOT_STARTED.getCode());
            } else if (LocalDateTime.now().isAfter(event.getEndTime())) {
                event.setStatus(EventStatusEnum.COMPLETED.getCode());
            } else {
                event.setStatus(EventStatusEnum.ACTIVE.getCode());
            }
            eventMapper.updateById(event);
        }

        return orderedEvents.stream()
                .map(
                        event -> {
                            EventRespVO vo = EventConvert.INSTANCE.DOconvertVO(event);
                            vo.setJoiningParticipants(
                                    countsByEventId.getOrDefault(event.getId(), 0));
                            vo.setGroups(groupsByEventId.getOrDefault(event.getId(), List.of()));
                            vo.setTaskStatus(
                                    taskStatusByEventId.getOrDefault(
                                            event.getId(), emptyTaskStatus()));
                            return vo;
                        })
                .toList();
    }

    @Override
    @Transactional
    public UpdateEventRespVO updateEvent(Long id, EventUpdateReqVO reqVO) {
        EventDO db = eventMapper.selectById(id);
        if (db == null) {
            throw exception(EVENT_NOT_FOUND);
        }

        runValidations(EventValidationContext.forUpdate(reqVO, db));

        EventDO patch = new EventDO();
        patch.setId(id);
        EventConvert.INSTANCE.patch(patch, reqVO);
        eventMapper.updateById(patch);

        EventDO updated = eventMapper.selectById(id);
        UpdateEventRespVO resp = EventConvert.INSTANCE.toUpdateResp(updated);

        resp.setParticipantUserIds(fetchParticipantIdsByEventId(id));

        return resp;
    }

    @Override
    @Transactional
    public Boolean deleteEvent(Long id) {
        EventDO db = eventMapper.selectById(id);
        if (db == null) {
            throw exception(EVENT_NOT_FOUND);
        }

        int rows = eventMapper.deleteById(id);
        if (rows <= 0) {
            throw exception(EVENT_DELETE_FAILED);
        }

        userGroupMapper.delete(Wrappers.<UserGroupDO>lambdaQuery().eq(UserGroupDO::getEventId, id));
        taskMapper.delete(Wrappers.<TaskDO>lambdaQuery().eq(TaskDO::getEventId, id));
        return true;
    }

    @Override
    @Transactional
    public Boolean restoreEvent(Long id) {
        EventDO db = eventMapper.selectRawById(id);
        if (db == null) {
            throw exception(EVENT_NOT_FOUND);
        }
        if (Boolean.FALSE.equals(db.getDeleted())) {
            throw exception(EVENT_NOT_DELETED);
        }

        int rows = eventMapper.restoreById(id);
        if (rows <= 0) {
            throw exception(EVENT_RESTORE_FAILED);
        }
        userGroupMapper.restoreByEventId(id);
        return true;
    }

    @Override
    public List<EventGroupRespVO> assignableMember(Long eventId) {
        List<DeptDO> groupList =
                deptMapper.selectList(
                        new LambdaQueryWrapper<DeptDO>().eq(DeptDO::getEventId, eventId));
        if (groupList == null || groupList.isEmpty()) {
            return Collections.emptyList();
        }
        List<EventGroupRespVO> groupRespVOList = new ArrayList<>();
        groupList.forEach(
                group -> {
                    List<GroupRespVO.MemberInfo> memberInfoList =
                            groupService.getGroupMembers(group.getId());
                    List<EventGroupRespVO.Member> memberList = new ArrayList<>();
                    memberInfoList.forEach(
                            member ->
                                    memberList.add(
                                            new EventGroupRespVO.Member(
                                                    member.getUserId(), member.getUsername())));
                    groupRespVOList.add(
                            new EventGroupRespVO(group.getId(), group.getName(), memberList));
                });
        return groupRespVOList;
    }

    private void runValidations(EventValidationContext context) {
        if (validationHandlers == null || validationHandlers.isEmpty()) {
            legacyValidate(context);
            return;
        }
        for (EventValidationHandler handler : validationHandlers) {
            if (handler.supports(context)) {
                handler.validate(context);
            }
        }
    }

    private void legacyValidate(EventValidationContext context) {
        validateTimeRange(context.getEffectiveStartTime(), context.getEffectiveEndTime());
        if (context.shouldValidateOrganizer()) {
            validateOrganizerExists(context.getRequestedOrganizerId());
        }
        List<Long> participantIds = context.getParticipantUserIds();
        if (participantIds != null) {
            validateParticipantsExist(participantIds);
        }
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && !start.isBefore(end)) {
            throw exception(TIME_RANGE_INVALID);
        }
    }

    private void validateOrganizerExists(Long organizerId) {
        if (organizerId == null || userMapper.selectById(organizerId) == null) {
            throw exception(ORGANIZER_NOT_FOUND);
        }
    }

    private void validateParticipantsExist(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<Long> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.size() != ids.size()) {
            throw exception(DUPLICATE_PARTICIPANTS);
        }

        List<Long> existIds =
                userMapper.selectBatchIds(distinct).stream().map(UserDO::getId).toList();

        if (existIds.size() != distinct.size()) {
            Set<Long> miss = new HashSet<>(distinct);
            miss.removeAll(new HashSet<>(existIds));
            log.warn("Missing participants: {}", miss);
            throw exception(PARTICIPANT_NOT_FOUND);
        }
    }

    private Map<Long, List<EventRespVO.GroupVO>> fetchGroupsByEventIds(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        List<DeptDO> groups =
                deptMapper.selectList(
                        Wrappers.<DeptDO>lambdaQuery()
                                .in(DeptDO::getEventId, eventIds)
                                .eq(DeptDO::getStatus, CommonStatusEnum.ENABLE.getStatus()));

        return groups.stream()
                .collect(
                        Collectors.groupingBy(
                                DeptDO::getEventId,
                                Collectors.mapping(this::toGroupVO, Collectors.toList())));
    }

    private Map<Long, EventRespVO.TaskStatusVO> fetchTaskStatusesByEventIds(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        List<TaskDO> tasks =
                taskMapper.selectList(
                        Wrappers.<TaskDO>lambdaQuery().in(TaskDO::getEventId, eventIds));

        Map<Long, List<TaskDO>> tasksByEvent =
                tasks.stream().collect(Collectors.groupingBy(TaskDO::getEventId));

        Map<Long, EventRespVO.TaskStatusVO> result = new HashMap<>();
        for (Long eventId : eventIds) {
            List<TaskDO> taskList = tasksByEvent.get(eventId);
            if (taskList == null || taskList.isEmpty()) {
                result.put(eventId, emptyTaskStatus());
            } else {
                result.put(eventId, toTaskStatusVO(taskList));
            }
        }
        return result;
    }

    private EventRespVO.GroupVO toGroupVO(DeptDO dept) {
        EventRespVO.GroupVO vo = new EventRespVO.GroupVO();
        vo.setId(String.valueOf(dept.getId()));
        vo.setName(dept.getName());
        return vo;
    }

    private EventRespVO.TaskStatusVO toTaskStatusVO(List<TaskDO> tasks) {
        EventRespVO.TaskStatusVO statusVO = new EventRespVO.TaskStatusVO();
        int total = tasks.size();
        int completed =
                (int)
                        tasks.stream()
                                .filter(
                                        task ->
                                                Objects.equals(
                                                        task.getStatus(),
                                                        TaskStatusEnum.COMPLETED.getStatus()))
                                .count();
        statusVO.setTotal(total);
        statusVO.setCompleted(completed);
        statusVO.setRemaining(total - completed);
        return statusVO;
    }

    private EventRespVO.TaskStatusVO emptyTaskStatus() {
        EventRespVO.TaskStatusVO statusVO = new EventRespVO.TaskStatusVO();
        statusVO.setTotal(0);
        statusVO.setCompleted(0);
        statusVO.setRemaining(0);
        return statusVO;
    }

    private Map<Long, Integer> fetchParticipantCountsByEventIds(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        List<UserGroupDO> userGroups =
                userGroupMapper.selectList(
                        Wrappers.<UserGroupDO>lambdaQuery().in(UserGroupDO::getEventId, eventIds));

        return userGroups.stream()
                .collect(
                        Collectors.groupingBy(
                                UserGroupDO::getEventId, Collectors.summingInt(item -> 1)));
    }

    private List<Long> fetchParticipantIdsByEventId(Long eventId) {
        if (eventId == null) {
            return List.of();
        }

        return userGroupMapper
                .selectList(
                        Wrappers.<UserGroupDO>lambdaQuery().eq(UserGroupDO::getEventId, eventId))
                .stream()
                .map(UserGroupDO::getUserId)
                .distinct()
                .toList();
    }
}
