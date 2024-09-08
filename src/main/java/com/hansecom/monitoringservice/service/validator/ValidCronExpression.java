package com.hansecom.monitoringservice.service.validator;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** Validates if it's a valid quartz cron expression. */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = ValidCronExpressionValidator.class)
public @interface ValidCronExpression {

  /**
   * Default constraint message.
   *
   * @return the default constraint message
   */
  String message() default "not a valid quartz cron expression";

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
