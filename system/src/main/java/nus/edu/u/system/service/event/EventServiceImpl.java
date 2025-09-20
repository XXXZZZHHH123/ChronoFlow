package nus.edu.u.system.service.event;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.*;

@Service
@Slf4j
public class EventServiceImpl implements EventService{
    @Resource
    private EventMapper eventMapper;
    @Resource
    private EventParticipantMapper eventParticipantMapper;
    @Resource
    private UserMapper userMapper;

    @Override
    @Transactional
    public Long createEvent(EventCreateReqVO reqVO) {
        validateTimeRange(reqVO.getStartTime(), reqVO.getEndTime());
        validateOrganizerExists(reqVO.getOrganizerId());
        validateParticipantsExist(reqVO.getParticipantUserIds());

        EventDTO dto = EventConvert.INSTANCE.convert(reqVO);
        EventDO event = EventConvert.INSTANCE.convert(dto);
        if (event.getStatus() == null) {
            event.setStatus(EventStatusEnum.NOT_STARTED.getCode());
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
        return event.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public EventRespVO getByEventId(Long eventId) {
        EventDO event = eventMapper.selectById(eventId);
        if (event == null) {
            throw exception(EVENT_NOT_FOUND);
        }
        EventRespVO resp = EventConvert.INSTANCE.DOconvertVO(event);

        List<EventParticipantDO> rels = eventParticipantMapper.selectList(
                new LambdaQueryWrapper<EventParticipantDO>().eq(EventParticipantDO::getEventId, eventId)
        );
        List<Long> participantIds = rels.stream()
                .map(EventParticipantDO::getUserId)
                .toList();
        resp.setParticipantUserIds(participantIds);
        return resp;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventRespVO> getByOrganizerId(Long organizerId) {
        if (userMapper.selectById(organizerId) == null) {
            throw exception(ORGANIZER_NOT_FOUND);
        }

        List<EventDO> events = eventMapper.selectList(
                new LambdaQueryWrapper<EventDO>().eq(EventDO::getUserId, organizerId)
        );
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream().map(EventDO::getId).toList();
        List<EventParticipantDO> allRels = eventParticipantMapper.selectList(
                new LambdaQueryWrapper<EventParticipantDO>().in(EventParticipantDO::getEventId, eventIds)
        );

        Map<Long, List<Long>> eventId2Users = allRels.stream()
                .collect(Collectors.groupingBy(
                        EventParticipantDO::getEventId,
                        Collectors.mapping(EventParticipantDO::getUserId, Collectors.toList())
                ));

        List<EventRespVO> list = EventConvert.INSTANCE.convertList(events);

        for (EventRespVO vo : list) {
            List<Long> participantIds = eventId2Users.getOrDefault(vo.getId(), Collections.emptyList());
            vo.setParticipantUserIds(participantIds);
        }
        return list;
    }

    @Override
    @Transactional
    public UpdateEventRespVO updateEvent(Long id, EventUpdateReqVO reqVO) {
        EventDO db = eventMapper.selectById(id);
        if (db == null) {
            throw exception(EVENT_NOT_FOUND);
        }

        validateTimeRange(reqVO.getStartTime(), reqVO.getEndTime());
        if(reqVO.getOrganizerId() != null) {
            validateOrganizerExists(reqVO.getOrganizerId());
        }
        validateParticipantsExist(reqVO.getParticipantUserIds());

        LambdaUpdateWrapper<EventDO> uw = Wrappers.<EventDO>lambdaUpdate().eq(EventDO::getId, id);
        if (reqVO.getEventName() != null) uw.set(EventDO::getName, reqVO.getEventName());
        if (reqVO.getDescription() != null) uw.set(EventDO::getDescription, reqVO.getDescription());
        if (reqVO.getOrganizerId() != null) uw.set(EventDO::getUserId, reqVO.getOrganizerId());
        if (reqVO.getStartTime() != null) uw.set(EventDO::getStartTime, reqVO.getStartTime());
        if (reqVO.getEndTime() != null) uw.set(EventDO::getEndTime, reqVO.getEndTime());
        if (reqVO.getStatus() != null) uw.set(EventDO::getStatus, reqVO.getStatus());
        if (reqVO.getRemark() != null) uw.set(EventDO::getRemark, reqVO.getRemark());
        eventMapper.update(new EventDO(), uw);

        if (reqVO.getParticipantUserIds() != null) {
            List<Long> targetList = reqVO.getParticipantUserIds().stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            List<EventParticipantDO> currentList = eventParticipantMapper.selectList(
                    Wrappers.<EventParticipantDO>lambdaQuery()
                            .eq(EventParticipantDO::getEventId, id)
            );
            Set<Long> current = currentList.stream()
                    .map(EventParticipantDO::getUserId)
                    .collect(Collectors.toSet());

            Set<Long> target = new HashSet<>(targetList);
            Set<Long> toRemove = new HashSet<>(current);
            toRemove.removeAll(target);
            Set<Long> toAdd = new HashSet<>(target);
            toAdd.removeAll(current);

            // if (reqVO.getOrganizerId() != null) {
            //     toAdd.remove(reqVO.getOrganizerId());
            //     toRemove.remove(reqVO.getOrganizerId());
            // }

            if (!toRemove.isEmpty()) {
                eventParticipantMapper.delete(
                        Wrappers.<EventParticipantDO>lambdaQuery()
                                .eq(EventParticipantDO::getEventId, id)
                                .in(EventParticipantDO::getUserId, toRemove)
                );
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

        UpdateEventRespVO resp = new UpdateEventRespVO();
        resp.setId(updated.getId());
        resp.setEventName(updated.getName());
        resp.setDescription(updated.getDescription());
        resp.setOrganizerId(updated.getUserId());
        resp.setStartTime(updated.getStartTime());
        resp.setEndTime(updated.getEndTime());
        resp.setStatus(updated.getStatus());
        resp.setRemarks(updated.getRemark());
        resp.setUpdateTime(updated.getUpdateTime());

        List<Long> participantIds = eventParticipantMapper.selectList(
                Wrappers.<EventParticipantDO>lambdaQuery().eq(EventParticipantDO::getEventId, id)
        ).stream().map(EventParticipantDO::getUserId).toList();
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
                Wrappers.<EventParticipantDO>lambdaQuery().eq(EventParticipantDO::getEventId, id)
        );

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

        List<Long> existIds = userMapper.selectBatchIds(distinct).stream()
                .map(UserDO::getId).toList();

        if (existIds.size() != distinct.size()) {
            Set<Long> miss = new HashSet<>(distinct);
            miss.removeAll(new HashSet<>(existIds));
            log.warn("Missing participants: {}", miss);
            throw exception(PARTICIPANT_NOT_FOUND);
        }
    }
}
