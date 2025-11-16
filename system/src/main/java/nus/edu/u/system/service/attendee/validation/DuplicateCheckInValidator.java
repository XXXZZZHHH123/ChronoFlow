package nus.edu.u.system.service.attendee.validation;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Duplicate Check-in Validator Validates that the attendee has not already checked in
 *
 * @author Fan Yazhuoting
 * @date 2025-10-15
 */
@Component
@Order(2)
@Slf4j
public class DuplicateCheckInValidator extends CheckInValidator {

    @Override
    protected void doValidate(CheckInValidationContext context) {
        log.debug("Validating duplicate check-in: {}", getValidatorName());

        if (ObjectUtil.equal(context.getAttendee().getCheckInStatus(), 1)) {
            log.warn(
                    "Attendee {} has already checked in", context.getAttendee().getAttendeeEmail());
            context.setValidationFailed(true);
            context.setErrorMessage("ALREADY_CHECKED_IN");
            return;
        }

        log.debug("Duplicate check-in validation passed");
    }

    @Override
    protected String getValidatorName() {
        return "DuplicateCheckInValidator";
    }
}
