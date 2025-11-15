package nus.edu.u.system.service.attendee.validation;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
import nus.edu.u.system.domain.dataobject.attendee.EventAttendeeDO;
import nus.edu.u.system.domain.dataobject.task.EventDO;

/**
 * Check-in Validation Context Holds all data needed during the validation chain execution
 *
 * @author Fan Yazhuoting
 * @date 2025-10-15
 */
@Data
@Builder
public class CheckInValidationContext {

    /** Check-in token from request */
    private String token;

    /** Current time for time window validation */
    private LocalDateTime currentTime;

    /** Attendee record (loaded by TokenValidator) */
    private EventAttendeeDO attendee;

    /** Event record (loaded by EventExistenceValidator) */
    private EventDO event;

    /** Flag to indicate if validation has failed */
    private boolean validationFailed;

    /** Error message if validation failed */
    private String errorMessage;
}
