package nus.edu.u.system.service.event.validation;

import static nus.edu.u.system.enums.ErrorCodeConstants.TIME_RANGE_INVALID;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import nus.edu.u.common.exception.ServiceException;
import nus.edu.u.system.domain.dataobject.task.EventDO;
import nus.edu.u.system.domain.vo.event.EventCreateReqVO;
import nus.edu.u.system.domain.vo.event.EventUpdateReqVO;
import org.junit.jupiter.api.Test;

class TimeRangeValidationHandlerTest {

    private final TimeRangeValidationHandler handler = new TimeRangeValidationHandler();

    @Test
    void supportsAlwaysReturnsTrue() {
        EventValidationContext context =
                createContext(LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        assertThat(handler.supports(context)).isTrue();
    }

    @Test
    void validate_allowsChronologicalRanges() {
        EventValidationContext context =
                createContext(
                        LocalDateTime.of(2025, 10, 1, 9, 0), LocalDateTime.of(2025, 10, 1, 11, 0));
        assertThatCode(() -> handler.validate(context)).doesNotThrowAnyException();
    }

    @Test
    void validate_rejectsEqualOrInvertedRanges() {
        LocalDateTime start = LocalDateTime.of(2025, 11, 1, 10, 0);
        EventValidationContext context = createContext(start, start);

        assertThatThrownBy(() -> handler.validate(context))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(TIME_RANGE_INVALID.getCode());
    }

    @Test
    void validate_usesExistingEventWhenUpdateHasNoTimes() {
        LocalDateTime start = LocalDateTime.of(2025, 12, 1, 9, 0);
        LocalDateTime end = start.plusHours(2);

        EventDO current = EventDO.builder().startTime(start).endTime(end).build();
        EventUpdateReqVO updateReqVO = new EventUpdateReqVO();
        EventValidationContext context = EventValidationContext.forUpdate(updateReqVO, current);

        assertThatCode(() -> handler.validate(context)).doesNotThrowAnyException();
    }

    private EventValidationContext createContext(LocalDateTime start, LocalDateTime end) {
        EventCreateReqVO reqVO = new EventCreateReqVO();
        reqVO.setStartTime(start);
        reqVO.setEndTime(end);
        return EventValidationContext.forCreate(reqVO);
    }
}
