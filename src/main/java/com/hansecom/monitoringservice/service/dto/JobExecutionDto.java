package com.hansecom.monitoringservice.service.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.UUID;

/** Data transfer object for Job Execution */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class JobExecutionDto {

  private UUID externalId;

  private String jobName;

  private String url;

  private JobExecutionStatus status;

  private String errorMessage;

  private Long responseTime;

  private Instant createdDate;
}
