package nus.edu.u.system.service.event.validation;

public interface EventValidationHandler {

    boolean supports(EventValidationContext context);

    void validate(EventValidationContext context);
}

