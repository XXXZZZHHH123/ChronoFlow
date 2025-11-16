package nus.edu.u.system.service.attendee.validation;

import cn.hutool.core.util.ObjectUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.system.domain.dataobject.attendee.EventAttendeeDO;
import nus.edu.u.system.mapper.attendee.EventAttendeeMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Token Validator Validates that the check-in token exists and retrieves attendee record
 *
 * @author Fan Yazhuoting
 * @date 2025-10-15
 */
@Component
@Order(1)
@Slf4j
public class TokenValidator extends CheckInValidator {

    @Resource private EventAttendeeMapper attendeeMapper;

    @Override
    protected void doValidate(CheckInValidationContext context) {
        log.debug("Validating token: {}", getValidatorName());

        EventAttendeeDO attendee = attendeeMapper.selectByToken(context.getToken());

        if (ObjectUtil.isNull(attendee)) {
            log.warn("Invalid check-in token");
            context.setValidationFailed(true);
            context.setErrorMessage("INVALID_CHECKIN_TOKEN");
            return;
        }

        // Set attendee in context for next validators
        context.setAttendee(attendee);
        log.debug("Token validation passed for attendee: {}", attendee.getAttendeeEmail());
    }

    @Override
    protected String getValidatorName() {
        return "TokenValidator";
    }
}
