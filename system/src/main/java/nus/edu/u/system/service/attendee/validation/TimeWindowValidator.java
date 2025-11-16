package nus.edu.u.system.service.attendee.validation;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Time Window Validator Validates that check-in occurs within allowed time window (2 hours before
 * event start until event end)
 *
 * @author Fan Yazhuoting
 * @date 2025-10-15
 */
@Component
@Order(5)
@Slf4j
public class TimeWindowValidator extends CheckInValidator {

    private static final int CHECK_IN_HOURS_BEFORE_START = 2;

    @Override
    protected void doValidate(CheckInValidationContext context) {
        log.debug("Validating time window: {}", getValidatorName());

        LocalDateTime now = context.getCurrentTime();
        LocalDateTime checkInStart =
                context.getEvent().getStartTime().minusHours(CHECK_IN_HOURS_BEFORE_START);
        LocalDateTime checkInEnd = context.getEvent().getEndTime();

        if (now.isBefore(checkInStart)) {
            log.warn(
                    "Check-in attempted too early. Current: {}, Allowed from: {}",
                    now,
                    checkInStart);
            context.setValidationFailed(true);
            context.setErrorMessage("CHECKIN_NOT_STARTED");
            return;
        }

        if (now.isAfter(checkInEnd)) {
            log.warn("Check-in attempted too late. Current: {}, Deadline: {}", now, checkInEnd);
            context.setValidationFailed(true);
            context.setErrorMessage("CHECKIN_ENDED");
            return;
        }

        log.debug(
                "Time window validation passed. Check-in allowed between {} and {}",
                checkInStart,
                checkInEnd);
    }

    @Override
    protected String getValidatorName() {
        return "TimeWindowValidator";
    }
}
