package nus.edu.u.system.service.event;

import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.enums.EventStatusEnum;
import nus.edu.u.system.convert.event.EventConvert;
import nus.edu.u.system.domain.dataobject.task.EventDO;
import nus.edu.u.system.domain.dataobject.task.EventParticipantDO;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.dto.EventDTO;
import nus.edu.u.system.domain.vo.event.*;
import nus.edu.u.system.mapper.task.EventMapper;
import nus.edu.u.system.mapper.task.EventParticipantMapper;
import nus.edu.u.system.mapper.user.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class EventServiceImpl implements EventService {
    @Resource private EventMapper eventMapper;
    @Resource private EventParticipantMapper eventParticipantMapper;
    @Resource private UserMapper userMapper;

    @Override
    @Transactional
    public EventRespVO createEvent(EventCreateReqVO reqVO) {
        validateTimeRange(reqVO.getStartTime(), reqVO.getEndTime());
        validateOrganizerExists(reqVO.getOrganizerId());
        validateParticipantsExist(reqVO.getParticipantUserIds());

        EventDTO dto = EventConvert.INSTANCE.convert(reqVO);
        EventDO event = EventConvert.INSTANCE.convert(dto);
        if (event.getStatus() == null) {
            event.setStatus(EventStatusEnum.ACTIVE.getCode());
        }
        eventMapper.insert(event);

        if (reqVO.getParticipantUserIds() != null) {
            for (Long userId : reqVO.getParticipantUserIds()) {
                EventParticipantDO relation = new EventParticipantDO();
                relation.setEventId(event.getId());
                relation.setUserId(userId);
                eventParticipantMapper.insert(relation);
            }
        }
        EventRespVO resp = EventConvert.INSTANCE.DOconvertVO(event);

        Long count =
                eventParticipantMapper.selectCount(
                        Wrappers.<EventParticipantDO>lambdaQuery()
                                .eq(EventParticipantDO::getEventId, event.getId()));
        resp.setJoiningParticipants(count.intValue());

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

        Long count =
                eventParticipantMapper.selectCount(
                        Wrappers.<EventParticipantDO>lambdaQuery()
                                .eq(EventParticipantDO::getEventId, eventId));
        resp.setJoiningParticipants(count.intValue());

        resp.setGroups(buildDefaultGroups());
        resp.setTaskStatus(buildDefaultTaskStatus());

        return resp;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventRespVO> getByOrganizerId(Long organizerId) {
        if (userMapper.selectById(organizerId) == null) {
            throw exception(ORGANIZER_NOT_FOUND);
        }

        List<EventDO> events =
                eventMapper.selectList(
                        new LambdaQueryWrapper<EventDO>().eq(EventDO::getUserId, organizerId));
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream().map(EventDO::getId).toList();
        List<EventParticipantDO> allRels =
                eventParticipantMapper.selectList(
                        new LambdaQueryWrapper<EventParticipantDO>()
                                .in(EventParticipantDO::getEventId, eventIds));

        Map<Long, Integer> countsByEventId =
                allRels.stream()
                        .collect(
                                Collectors.groupingBy(
                                        EventParticipantDO::getEventId,
                                        Collectors.summingInt(e -> 1)));

        return events.stream()
                .map(
                        event -> {
                            EventRespVO vo = EventConvert.INSTANCE.DOconvertVO(event);
                            vo.setJoiningParticipants(
                                    countsByEventId.getOrDefault(event.getId(), 0));
                            vo.setGroups(buildDefaultGroups());
                            vo.setTaskStatus(buildDefaultTaskStatus());
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

        LocalDateTime start =
                reqVO.getStartTime() != null ? reqVO.getStartTime() : db.getStartTime();
        LocalDateTime end = reqVO.getEndTime() != null ? reqVO.getEndTime() : db.getEndTime();
        validateTimeRange(start, end);

        if (reqVO.getOrganizerId() != null) {
            validateOrganizerExists(reqVO.getOrganizerId());
        }
        validateParticipantsExist(reqVO.getParticipantUserIds());

        EventDO patch = new EventDO();
        patch.setId(id);
        EventConvert.INSTANCE.patch(patch, reqVO);
        eventMapper.updateById(patch);

        if (reqVO.getParticipantUserIds() != null) {
            List<Long> targetList =
                    reqVO.getParticipantUserIds().stream()
                            .filter(Objects::nonNull)
                            .distinct()
                            .toList();

            List<EventParticipantDO> currentList =
                    eventParticipantMapper.selectList(
                            Wrappers.<EventParticipantDO>lambdaQuery()
                                    .eq(EventParticipantDO::getEventId, id));
            Set<Long> current =
                    currentList.stream()
                            .map(EventParticipantDO::getUserId)
                            .collect(Collectors.toSet());

            Set<Long> target = new HashSet<>(targetList);
            Set<Long> toRemove = new HashSet<>(current);
            toRemove.removeAll(target);
            Set<Long> toAdd = new HashSet<>(target);
            toAdd.removeAll(current);

            if (!toRemove.isEmpty()) {
                eventParticipantMapper.delete(
                        Wrappers.<EventParticipantDO>lambdaQuery()
                                .eq(EventParticipantDO::getEventId, id)
                                .in(EventParticipantDO::getUserId, toRemove));
            }

            if (!toAdd.isEmpty()) {
                for (Long uid : toAdd) {
                    EventParticipantDO rel = new EventParticipantDO();
                    rel.setEventId(id);
                    rel.setUserId(uid);
                    eventParticipantMapper.insert(rel);
                }
            }
        }

        EventDO updated = eventMapper.selectById(id);
        UpdateEventRespVO resp = EventConvert.INSTANCE.toUpdateResp(updated);

        List<Long> participantIds =
                eventParticipantMapper
                        .selectList(
                                Wrappers.<EventParticipantDO>lambdaQuery()
                                        .eq(EventParticipantDO::getEventId, id))
                        .stream()
                        .map(EventParticipantDO::getUserId)
                        .toList();
        resp.setParticipantUserIds(participantIds);

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

        eventParticipantMapper.delete(
                Wrappers.<EventParticipantDO>lambdaQuery().eq(EventParticipantDO::getEventId, id));

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
        eventParticipantMapper.restoreByEventId(id);
        return true;
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
        if (ids == null || ids.isEmpty()) return;

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

    private List<EventRespVO.GroupVO> buildDefaultGroups() {
        EventRespVO.GroupVO g1 = new EventRespVO.GroupVO();
        g1.setId("grp_a");
        g1.setName("Logistics");

        EventRespVO.GroupVO g2 = new EventRespVO.GroupVO();
        g2.setId("grp_b");
        g2.setName("Registration");

        return List.of(g1, g2);
    }

    private EventRespVO.TaskStatusVO buildDefaultTaskStatus() {
        EventRespVO.TaskStatusVO ts = new EventRespVO.TaskStatusVO();
        ts.setTotal(0);
        ts.setRemaining(0);
        ts.setCompleted(0);
        return ts;
    }
}
