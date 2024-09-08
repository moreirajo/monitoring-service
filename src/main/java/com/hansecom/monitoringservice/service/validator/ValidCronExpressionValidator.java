package com.hansecom.monitoringservice.service.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.quartz.CronExpression;

/**
 * Validator for {@link ValidCronExpression} annotation.
 *
 * <p>Validates if it's a valid quartz cron expression.
 */
public class ValidCronExpressionValidator
    implements ConstraintValidator<ValidCronExpression, String> {

  @Override
  public void initialize(ValidCronExpression parameters) {
    // Nothing to do here
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
    return value != null && CronExpression.isValidExpression(value);
  }
}
