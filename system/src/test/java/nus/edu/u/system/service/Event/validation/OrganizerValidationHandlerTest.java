package nus.edu.u.system.service.event.validation;

import static nus.edu.u.system.enums.ErrorCodeConstants.ORGANIZER_NOT_FOUND;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import nus.edu.u.common.exception.ServiceException;
import nus.edu.u.system.domain.dataobject.task.EventDO;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.vo.event.EventCreateReqVO;
import nus.edu.u.system.domain.vo.event.EventUpdateReqVO;
import nus.edu.u.system.mapper.user.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizerValidationHandlerTest {

    @Mock private UserMapper userMapper;

    @InjectMocks private OrganizerValidationHandler handler;

    private EventCreateReqVO baseCreateRequest() {
        EventCreateReqVO reqVO = new EventCreateReqVO();
        reqVO.setOrganizerId(10L);
        reqVO.setStartTime(LocalDateTime.of(2025, 10, 1, 10, 0));
        reqVO.setEndTime(reqVO.getStartTime().plusHours(1));
        return reqVO;
    }

    private EventValidationContext updateContext(Long organizerId) {
        EventUpdateReqVO reqVO = new EventUpdateReqVO();
        reqVO.setOrganizerId(organizerId);

        EventDO current =
                EventDO.builder()
                        .startTime(LocalDateTime.of(2025, 9, 1, 9, 0))
                        .endTime(LocalDateTime.of(2025, 9, 1, 12, 0))
                        .build();
        return EventValidationContext.forUpdate(reqVO, current);
    }

    @Test
    void supports_trueForCreateAndUpdateWithOrganizer() {
        EventValidationContext createContext =
                EventValidationContext.forCreate(baseCreateRequest());
        assertThat(handler.supports(createContext)).isTrue();

        EventValidationContext updateWithoutOrganizer = updateContext(null);
        assertThat(handler.supports(updateWithoutOrganizer)).isFalse();

        EventValidationContext updateWithOrganizer = updateContext(99L);
        assertThat(handler.supports(updateWithOrganizer)).isTrue();
    }

    @Test
    void validate_passesWhenOrganizerExists() {
        when(userMapper.selectById(10L)).thenReturn(new UserDO());

        EventValidationContext context = EventValidationContext.forCreate(baseCreateRequest());

        assertThatCode(() -> handler.validate(context)).doesNotThrowAnyException();
    }

    @Test
    void validate_throwsWhenOrganizerMissing() {
        when(userMapper.selectById(10L)).thenReturn(null);

        EventValidationContext context = EventValidationContext.forCreate(baseCreateRequest());

        assertThatThrownBy(() -> handler.validate(context))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(ORGANIZER_NOT_FOUND.getCode());
    }
}
