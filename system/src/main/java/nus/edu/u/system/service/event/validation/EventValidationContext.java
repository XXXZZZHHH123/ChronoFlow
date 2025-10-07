package nus.edu.u.system.service.event.validation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import nus.edu.u.system.domain.dataobject.task.EventDO;
import nus.edu.u.system.domain.vo.event.EventCreateReqVO;
import nus.edu.u.system.domain.vo.event.EventUpdateReqVO;

public final class EventValidationContext {

    private final EventOperationType operationType;
    private final EventCreateReqVO createReqVO;
    private final EventUpdateReqVO updateReqVO;
    private final EventDO currentEvent;
    private final LocalDateTime effectiveStartTime;
    private final LocalDateTime effectiveEndTime;
    private final Long organizerId;
    private final boolean organizerProvided;
    private final List<Long> participantUserIds;

    private EventValidationContext(
            EventOperationType operationType,
            EventCreateReqVO createReqVO,
            EventUpdateReqVO updateReqVO,
            EventDO currentEvent,
            LocalDateTime effectiveStartTime,
            LocalDateTime effectiveEndTime,
            Long organizerId,
            boolean organizerProvided,
            List<Long> participantUserIds) {
        this.operationType = operationType;
        this.createReqVO = createReqVO;
        this.updateReqVO = updateReqVO;
        this.currentEvent = currentEvent;
        this.effectiveStartTime = effectiveStartTime;
        this.effectiveEndTime = effectiveEndTime;
        this.organizerId = organizerId;
        this.organizerProvided = organizerProvided;
        this.participantUserIds = participantUserIds;
    }

    public static EventValidationContext forCreate(EventCreateReqVO reqVO) {
        List<Long> participantIds =
                reqVO.getParticipantUserIds() != null
                        ? new ArrayList<>(reqVO.getParticipantUserIds())
                        : null;
        return new EventValidationContext(
                EventOperationType.CREATE,
                reqVO,
                null,
                null,
                reqVO.getStartTime(),
                reqVO.getEndTime(),
                reqVO.getOrganizerId(),
                true,
                participantIds);
    }

    public static EventValidationContext forUpdate(EventUpdateReqVO reqVO, EventDO currentEvent) {
        Objects.requireNonNull(currentEvent, "currentEvent must not be null");
        LocalDateTime effectiveStart =
                reqVO.getStartTime() != null ? reqVO.getStartTime() : currentEvent.getStartTime();
        LocalDateTime effectiveEnd =
                reqVO.getEndTime() != null ? reqVO.getEndTime() : currentEvent.getEndTime();
        List<Long> participantIds =
                reqVO.getParticipantUserIds() != null
                        ? new ArrayList<>(reqVO.getParticipantUserIds())
                        : null;
        return new EventValidationContext(
                EventOperationType.UPDATE,
                null,
                reqVO,
                currentEvent,
                effectiveStart,
                effectiveEnd,
                reqVO.getOrganizerId(),
                reqVO.getOrganizerId() != null,
                participantIds);
    }

    public EventOperationType getOperationType() {
        return operationType;
    }

    public Optional<EventCreateReqVO> getCreateReqVO() {
        return Optional.ofNullable(createReqVO);
    }

    public Optional<EventUpdateReqVO> getUpdateReqVO() {
        return Optional.ofNullable(updateReqVO);
    }

    public Optional<EventDO> getCurrentEvent() {
        return Optional.ofNullable(currentEvent);
    }

    public LocalDateTime getEffectiveStartTime() {
        return effectiveStartTime;
    }

    public LocalDateTime getEffectiveEndTime() {
        return effectiveEndTime;
    }

    public boolean shouldValidateOrganizer() {
        return operationType == EventOperationType.CREATE || organizerProvided;
    }

    public Long getRequestedOrganizerId() {
        return organizerId;
    }

    public List<Long> getParticipantUserIds() {
        return participantUserIds;
    }
}
