package com.hansecom.monitoringservice.persistence.repository;

import com.hansecom.monitoringservice.persistence.model.JobExecutionCriteriaParams;
import com.hansecom.monitoringservice.persistence.model.JobExecutionEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;

/** Job execution repository with custom queries. */
@Validated
public interface JobExecutionRepositoryCustom {

  /**
   * Method that performs dynamic queries according to the input.
   *
   * @param jobExecutionCriteriaParams object holding criteria parameters
   * @return a {@link Page} containing the results and pagination information
   */
  Page<JobExecutionEntity> findByCriteria(
      @NotNull @Valid JobExecutionCriteriaParams jobExecutionCriteriaParams);
}
