package com.hansecom.monitoringservice.service.dto;

import com.hansecom.monitoringservice.exception.StandardException;
import com.hansecom.monitoringservice.rest.inbound.openapi.model.InvalidParamDetail;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Data transfer object for pagination.
 *
 * @param pageNumber the page number
 * @param pageSize the page size
 * @param sortDirection the sort direction
 * @param sortProperties the sort properties
 */
public record PageRequestDto(
    @PositiveOrZero(message = "page number {jakarta.validation.constraints.PositiveOrZero.message}")
        Integer pageNumber,
    @Positive(message = "page size {jakarta.validation.constraints.Positive.message}")
        Integer pageSize,
    SortDirection sortDirection,
    Set<String> sortProperties) {

  /** Sort direction possible values. */
  public enum SortDirection {
    ASC,
    DESC;

    /**
     * Returns the corresponding {@link SortDirection} from the input string.
     *
     * <p>An error will be thrown if no matches found.
     *
     * @param value the sort direction value
     * @return The corresponding {@link SortDirection} if exists, otherwise returns an exception
     * @throws StandardException if input don't match any value
     */
    public static SortDirection fromString(String value) {

      String errorMessage =
          "Invalid sort direction value. Please provide one of %s"
              .formatted(Arrays.toString(SortDirection.values()));

      return Arrays.stream(values())
          .filter(v -> v.name().equals(value.toUpperCase()))
          .findFirst()
          .orElseThrow(
              () ->
                  StandardException.invalidRequestParams(
                      new IllegalArgumentException(errorMessage),
                      List.of(
                          new InvalidParamDetail().name("sort-direction").reason(errorMessage))));
    }
  }
}
