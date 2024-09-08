package com.hansecom.monitoringservice.service.validator;

import com.hansecom.monitoringservice.service.dto.PageRequestDto;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Validates {@link PageRequestDto} object. */
@Documented
@Constraint(validatedBy = ValidPageRequestDtoValidator.class)
@Target({ElementType.TYPE_USE, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPageRequestDto {

  /**
   * Default constraint message.
   *
   * @return the default constraint message
   */
  String message() default "Invalid field";

  /**
   * Class to validate to check if the field exists.
   *
   * @return the class
   */
  Class<?> clazz();

  /**
   * Default groups.
   *
   * @return the default groups
   */
  Class<?>[] groups() default {};

  /**
   * Default payload.
   *
   * @return the default payload
   */
  Class<? extends Payload>[] payload() default {};
}
