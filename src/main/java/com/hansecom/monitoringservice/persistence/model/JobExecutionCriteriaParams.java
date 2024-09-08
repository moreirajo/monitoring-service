package com.hansecom.monitoringservice.persistence.model;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.domain.Pageable;

/** Data transfer object holding criteria filter parameters. */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class JobExecutionCriteriaParams {

  @NotNull private Optional<String> jobName = Optional.empty();

  @NotNull private Optional<String> url = Optional.empty();

  @NotNull private Optional<JobExecutionStatusEntity> status = Optional.empty();

  @NotNull private Optional<Instant> from = Optional.empty();

  @NotNull private Optional<Instant> to = Optional.empty();

  @NotNull private Pageable pageable = Pageable.unpaged();
}
