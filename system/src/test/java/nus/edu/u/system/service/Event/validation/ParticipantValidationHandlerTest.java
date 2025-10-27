package nus.edu.u.system.service.event.validation;

import static nus.edu.u.system.enums.ErrorCodeConstants.DUPLICATE_PARTICIPANTS;
import static nus.edu.u.system.enums.ErrorCodeConstants.PARTICIPANT_NOT_FOUND;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import nus.edu.u.common.exception.ServiceException;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.vo.event.EventCreateReqVO;
import nus.edu.u.system.mapper.user.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParticipantValidationHandlerTest {

    @Mock private UserMapper userMapper;

    @InjectMocks private ParticipantValidationHandler handler;

    private EventCreateReqVO newCreateRequest(List<Long> participants) {
        EventCreateReqVO reqVO = new EventCreateReqVO();
        reqVO.setParticipantUserIds(participants);
        reqVO.setStartTime(LocalDateTime.of(2025, 10, 1, 9, 0));
        reqVO.setEndTime(reqVO.getStartTime().plusHours(2));
        return reqVO;
    }

    @Test
    void supports_onlyWhenParticipantsProvided() {
        EventCreateReqVO noParticipants = newCreateRequest(null);
        assertThat(handler.supports(EventValidationContext.forCreate(noParticipants))).isFalse();

        EventCreateReqVO withParticipants = newCreateRequest(List.of(1L));
        assertThat(handler.supports(EventValidationContext.forCreate(withParticipants))).isTrue();
    }

    @Test
    void validate_allParticipantsExist() {
        List<Long> ids = List.of(1L, 2L);
        EventValidationContext context = EventValidationContext.forCreate(newCreateRequest(ids));

        when(userMapper.selectBatchIds(anyCollection()))
                .thenReturn(
                        List.of(UserDO.builder().id(1L).build(), UserDO.builder().id(2L).build()));

        assertThatCode(() -> handler.validate(context)).doesNotThrowAnyException();
    }

    @Test
    void validate_rejectsDuplicates() {
        EventValidationContext context =
                EventValidationContext.forCreate(newCreateRequest(List.of(1L, 1L)));

        assertThatThrownBy(() -> handler.validate(context))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(DUPLICATE_PARTICIPANTS.getCode());
    }

    @Test
    void validate_rejectsWhenSomeParticipantsMissing() {
        List<Long> ids = List.of(1L, 2L);
        EventValidationContext context = EventValidationContext.forCreate(newCreateRequest(ids));

        when(userMapper.selectBatchIds(anyCollection()))
                .thenReturn(List.of(UserDO.builder().id(1L).build()));

        assertThatThrownBy(() -> handler.validate(context))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(PARTICIPANT_NOT_FOUND.getCode());
    }
}
