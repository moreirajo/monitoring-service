package com.hansecom.monitoringservice.service.validator;

import com.hansecom.monitoringservice.service.dto.PageRequestDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ReflectionUtils;

/**
 * Validator for {@link ValidPageRequestDto} annotation.
 *
 * <p>Validates the page size and if the sort properties exists for the given class, set through the
 * annotation property {@code clazz}.
 */
public class ValidPageRequestDtoValidator
    implements ConstraintValidator<ValidPageRequestDto, PageRequestDto> {

  @Value("${custom-properties.database.pagination.max-page-size:100}")
  private int maxPageSize;

  private Class<?> clazz;

  @Override
  public void initialize(ValidPageRequestDto constraintAnnotation) {
    clazz = constraintAnnotation.clazz();
  }

  @Override
  public boolean isValid(PageRequestDto value, ConstraintValidatorContext context) {

    boolean isValid = true;

    if (value != null) {

      context.disableDefaultConstraintViolation();

      if (value.pageSize() != null && value.pageSize() > maxPageSize) {
        isValid = false;
        context
            .buildConstraintViolationWithTemplate(
                "page size max value allowed is %s".formatted(maxPageSize))
            .addPropertyNode("pageSize")
            .addConstraintViolation();
      }

      if (value.sortProperties() != null) {
        for (String property : value.sortProperties()) {
          if (ReflectionUtils.findField(clazz, property) == null) {
            isValid = false;
            context
                .buildConstraintViolationWithTemplate("invalid field")
                .addPropertyNode("sortProperties." + property)
                .addConstraintViolation();
          }
        }
      }
    }

    return isValid;
  }
}
