package nus.edu.u.system.service.attendee.validation;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.system.enums.event.EventStatusEnum;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Event Status Validator
 * Validates that the event is in ACTIVE status
 *
 * @author Fan Yazhuoting
 * @date 2025-10-15
 */
@Component
@Order(4)
@Slf4j
public class EventStatusValidator extends CheckInValidator {

    @Override
    protected void doValidate(CheckInValidationContext context) {
        log.debug("Validating event status: {}", getValidatorName());

        if (!ObjectUtil.equal(context.getEvent().getStatus(), EventStatusEnum.ACTIVE.getCode())) {
            log.warn("Event {} is not active, current status: {}",
                    context.getEvent().getName(),
                    context.getEvent().getStatus());
            context.setValidationFailed(true);
            context.setErrorMessage("EVENT_NOT_ACTIVE");
            return;
        }

        log.debug("Event status validation passed");
    }

    @Override
    protected String getValidatorName() {
        return "EventStatusValidator";
    }
}