package nus.edu.u.system.service.event.validation;

import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.ORGANIZER_NOT_FOUND;

import jakarta.annotation.Resource;
import nus.edu.u.system.mapper.user.UserMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class OrganizerValidationHandler implements EventValidationHandler {

    @Resource private UserMapper userMapper;

    @Override
    public boolean supports(EventValidationContext context) {
        return context.shouldValidateOrganizer();
    }

    @Override
    public void validate(EventValidationContext context) {
        Long organizerId = context.getRequestedOrganizerId();
        if (organizerId == null || userMapper.selectById(organizerId) == null) {
            throw exception(ORGANIZER_NOT_FOUND);
        }
    }
}
