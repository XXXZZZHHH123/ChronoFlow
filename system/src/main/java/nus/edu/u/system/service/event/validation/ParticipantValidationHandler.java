package nus.edu.u.system.service.event.validation;

import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.DUPLICATE_PARTICIPANTS;
import static nus.edu.u.system.enums.ErrorCodeConstants.PARTICIPANT_NOT_FOUND;

import jakarta.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.mapper.user.UserMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
@Slf4j
public class ParticipantValidationHandler implements EventValidationHandler {

    @Resource private UserMapper userMapper;

    @Override
    public boolean supports(EventValidationContext context) {
        List<Long> ids = context.getParticipantUserIds();
        return ids != null && !ids.isEmpty();
    }

    @Override
    public void validate(EventValidationContext context) {
        List<Long> ids = context.getParticipantUserIds();
        if (ids == null || ids.isEmpty()) {
            return;
        }

        List<Long> distinct =
                ids.stream().filter(id -> id != null).distinct().collect(Collectors.toList());
        if (distinct.size() != ids.size()) {
            throw exception(DUPLICATE_PARTICIPANTS);
        }

        List<Long> existIds =
                userMapper.selectBatchIds(distinct).stream().map(UserDO::getId).toList();

        if (existIds.size() != distinct.size()) {
            Set<Long> missing = new HashSet<>(distinct);
            missing.removeAll(new HashSet<>(existIds));
            log.warn("Missing participants: {}", missing);
            throw exception(PARTICIPANT_NOT_FOUND);
        }
    }
}

