package nus.edu.u.common.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;
import nus.edu.u.common.validation.MobileValidator;

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
@Constraint(validatedBy = MobileValidator.class)
public @interface Mobile {

  String message() default "The mobile phone number format is incorrect";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
