package nus.edu.u.system.service.attendee.validation;

import cn.hutool.core.util.ObjectUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.system.domain.dataobject.task.EventDO;
import nus.edu.u.system.mapper.task.EventMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Event Existence Validator
 * Validates that the event exists and retrieves event record
 *
 * @author Fan Yazhuoting
 * @date 2025-10-15
 */
@Component
@Order(3)
@Slf4j
public class EventExistenceValidator extends CheckInValidator {

    @Resource
    private EventMapper eventMapper;

    @Override
    protected void doValidate(CheckInValidationContext context) {
        log.debug("Validating event existence: {}", getValidatorName());

        EventDO event = eventMapper.selectById(context.getAttendee().getEventId());

        if (ObjectUtil.isNull(event)) {
            log.warn("Event not found: {}", context.getAttendee().getEventId());
            context.setValidationFailed(true);
            context.setErrorMessage("EVENT_NOT_FOUND");
            return;
        }

        // Set event in context for next validators
        context.setEvent(event);
        log.debug("Event existence validation passed for event: {}", event.getName());
    }

    @Override
    protected String getValidatorName() {
        return "EventExistenceValidator";
    }
}