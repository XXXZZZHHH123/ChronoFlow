package nus.edu.u.system.service.attendee.validation;

import jakarta.annotation.Resource;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Check-in Validation Chain Builder Builds and manages the chain of responsibility for check-in
 * validation
 *
 * @author Fan Yazhuoting
 * @date 2025-10-15
 */
@Component
@Slf4j
public class CheckInValidationChainBuilder {

    @Resource private TokenValidator tokenValidator;

    @Resource private DuplicateCheckInValidator duplicateCheckInValidator;

    @Resource private EventExistenceValidator eventExistenceValidator;

    @Resource private EventStatusValidator eventStatusValidator;

    @Resource private TimeWindowValidator timeWindowValidator;

    /**
     * Build the validation chain with all validators in order
     *
     * @return The head of the validation chain
     */
    public CheckInValidator buildValidationChain() {
        log.debug("Building check-in validation chain");

        // Build the chain: Token -> Duplicate -> Event Existence -> Event Status -> Time Window
        tokenValidator
                .setNext(duplicateCheckInValidator)
                .setNext(eventExistenceValidator)
                .setNext(eventStatusValidator)
                .setNext(timeWindowValidator);

        log.debug("Validation chain built successfully with {} validators", 5);
        return tokenValidator;
    }

    /**
     * Alternative method: Build chain from a list of validators This allows for dynamic chain
     * composition in the future
     *
     * @param validators List of validators in desired order
     * @return The head of the validation chain
     */
    public CheckInValidator buildValidationChain(List<CheckInValidator> validators) {
        if (validators == null || validators.isEmpty()) {
            throw new IllegalArgumentException("Validators list cannot be null or empty");
        }

        log.debug("Building custom validation chain with {} validators", validators.size());

        CheckInValidator head = validators.get(0);
        CheckInValidator current = head;

        for (int i = 1; i < validators.size(); i++) {
            current = current.setNext(validators.get(i));
        }

        return head;
    }
}
