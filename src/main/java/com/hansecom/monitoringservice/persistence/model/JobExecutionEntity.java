package com.hansecom.monitoringservice.persistence.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Database entity representation. */
@SuperBuilder(toBuilder = true)
@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "job_execution")
public class JobExecutionEntity extends AbstractModelEntity {

  private String jobName;

  private String url;

  @Enumerated(EnumType.STRING)
  private JobExecutionStatusEntity status;

  private Long responseTime;

  private String errorMessage;
}
