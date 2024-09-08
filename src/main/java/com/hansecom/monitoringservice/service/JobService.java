package com.hansecom.monitoringservice.service;

import com.hansecom.monitoringservice.service.dto.JobDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

/** Handles business logic for job. */
@Validated
public interface JobService {

  /**
   * Creates a new job.
   *
   * @param jobDto job properties
   * @return the newly created job
   */
  JobDto createJob(@NotNull @Valid final JobDto jobDto);
}
