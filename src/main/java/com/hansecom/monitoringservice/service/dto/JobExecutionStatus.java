package com.hansecom.monitoringservice.service.dto;

import com.hansecom.monitoringservice.exception.StandardException;
import com.hansecom.monitoringservice.rest.inbound.openapi.model.InvalidParamDetail;
import java.util.Arrays;
import java.util.List;

/** Possible values for Job Execution status. */
public enum JobExecutionStatus {
  SUCCEEDED,
  FAILED;

  /**
   * Returns the corresponding {@link JobExecutionStatus} from the input string.
   *
   * <p>An error will be thrown if no matches found.
   *
   * @param value the status value
   * @return The corresponding {@link JobExecutionStatus} if exists, otherwise returns an exception
   * @throws StandardException if input don't match any value
   */
  public static JobExecutionStatus fromString(String value) {

    String errorMessage =
        "Invalid status value. Please provide one of %s"
            .formatted(Arrays.toString(JobExecutionStatus.values()));

    return Arrays.stream(values())
        .filter(v -> v.name().equals(value.toUpperCase()))
        .findFirst()
        .orElseThrow(
            () ->
                StandardException.invalidRequestParams(
                    new IllegalArgumentException(errorMessage),
                    List.of(new InvalidParamDetail().name("status").reason(errorMessage))));
  }
}
