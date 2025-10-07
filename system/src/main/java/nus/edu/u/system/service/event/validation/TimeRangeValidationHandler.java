package nus.edu.u.system.service.event.validation;

import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.TIME_RANGE_INVALID;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class TimeRangeValidationHandler implements EventValidationHandler {

    @Override
    public boolean supports(EventValidationContext context) {
        return true;
    }

    @Override
    public void validate(EventValidationContext context) {
        if (context.getEffectiveStartTime() != null
                && context.getEffectiveEndTime() != null
                && !context.getEffectiveStartTime().isBefore(context.getEffectiveEndTime())) {
            throw exception(TIME_RANGE_INVALID);
        }
    }
}
