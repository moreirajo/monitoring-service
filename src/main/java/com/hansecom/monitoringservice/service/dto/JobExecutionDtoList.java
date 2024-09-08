package com.hansecom.monitoringservice.service.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** Data transfer object for a list of Jobs. */
@Getter
@Builder
public class JobExecutionDtoList {

  private List<JobExecutionDto> jobExecutionList;

  private int totalPages;

  private long totalElements;
}
