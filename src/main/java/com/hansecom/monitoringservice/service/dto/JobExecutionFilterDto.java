package com.hansecom.monitoringservice.service.dto;

import com.hansecom.monitoringservice.persistence.model.JobExecutionEntity;
import com.hansecom.monitoringservice.service.validator.ValidPageRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

/** Data transfer object for filtering Job Executions. */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class JobExecutionFilterDto {

  @NotNull private Optional<String> jobName;

  @NotNull
  private Optional<@URL(message = "url {org.hibernate.validator.constraints.URL.message}") String>
      url;

  @NotNull private Optional<JobExecutionStatus> status;

  @NotNull private Optional<Instant> from;

  @NotNull private Optional<Instant> to;

  @NotNull
  private Optional<@Valid @ValidPageRequestDto(clazz = JobExecutionEntity.class) PageRequestDto>
      pageRequestDto;
}
