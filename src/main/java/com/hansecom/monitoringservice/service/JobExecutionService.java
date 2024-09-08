package com.hansecom.monitoringservice.service;

import com.hansecom.monitoringservice.service.dto.JobExecutionDtoList;
import com.hansecom.monitoringservice.service.dto.JobExecutionFilterDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

/** Handles business logic for job executions. */
@Validated
public interface JobExecutionService {

  /**
   * Get all Job Executions, Supports filtering and pagination.
   *
   * @param jobExecutionFilterDto filters to apply in the query
   * @return a list of all job executions found according to the filters
   */
  JobExecutionDtoList getJobsExecutions(
      @NotNull @Valid final JobExecutionFilterDto jobExecutionFilterDto);
}
