package com.hansecom.monitoringservice.service.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.ZoneId;

/**
 * Validator for {@link ValidTimezone} annotation.
 *
 * <p>Validates if it's a valid timezone.
 */
public class ValidTimezoneValidator implements ConstraintValidator<ValidTimezone, String> {

  @Override
  public void initialize(ValidTimezone parameters) {
    // Nothing to do here
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
    return value == null || ZoneId.getAvailableZoneIds().contains(value);
  }
}
