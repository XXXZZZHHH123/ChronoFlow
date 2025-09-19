package nus.edu.u.common.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;
import nus.edu.u.common.core.ArrayValuable;
import nus.edu.u.common.validation.InEnumCollectionValidator;
import nus.edu.u.common.validation.InEnumValidator;

@Target({
    ElementType.METHOD,
    ElementType.FIELD,
    ElementType.ANNOTATION_TYPE,
    ElementType.CONSTRUCTOR,
    ElementType.PARAMETER,
    ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {InEnumValidator.class, InEnumCollectionValidator.class})
public @interface InEnum {

    /**
     * @return Class that implement the ArrayValuable interface
     */
    Class<? extends ArrayValuable<?>> value();

    String message() default "Must be included {value}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
