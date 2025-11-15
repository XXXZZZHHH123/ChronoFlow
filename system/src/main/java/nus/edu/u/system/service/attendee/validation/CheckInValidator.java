package nus.edu.u.system.service.attendee.validation;

/**
 * Abstract Check-in Validator Base class for all check-in validation rules using Chain of
 * Responsibility pattern
 *
 * @author Fan Yazhuoting
 * @date 2025-10-15
 */
public abstract class CheckInValidator {

    protected CheckInValidator next;

    /**
     * Set the next validator in the chain
     *
     * @param next Next validator
     * @return The next validator (for fluent API)
     */
    public CheckInValidator setNext(CheckInValidator next) {
        this.next = next;
        return next;
    }

    /**
     * Validate the check-in request If validation passes, proceed to next validator in chain
     *
     * @param context Validation context containing all necessary data
     */
    public void validate(CheckInValidationContext context) {
        // Perform this validator's specific validation
        doValidate(context);

        // If validation failed, stop the chain
        if (context.isValidationFailed()) {
            return;
        }

        // Continue to next validator if exists
        if (next != null) {
            next.validate(context);
        }
    }

    /**
     * Perform the specific validation logic for this validator Subclasses must implement this
     * method
     *
     * @param context Validation context
     */
    protected abstract void doValidate(CheckInValidationContext context);

    /**
     * Get the validator name for logging
     *
     * @return Validator name
     */
    protected abstract String getValidatorName();
}
