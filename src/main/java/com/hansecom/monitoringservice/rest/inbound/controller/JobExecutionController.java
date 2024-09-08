package com.hansecom.monitoringservice.rest.inbound.controller;

import com.hansecom.monitoringservice.rest.inbound.openapi.api.JobsExecutionApi;
import com.hansecom.monitoringservice.rest.inbound.openapi.model.JobExecutionResponseList;
import com.hansecom.monitoringservice.service.JobExecutionService;
import com.hansecom.monitoringservice.service.dto.JobExecutionDtoList;
import com.hansecom.monitoringservice.service.dto.JobExecutionFilterDto;
import com.hansecom.monitoringservice.service.dto.JobExecutionStatus;
import com.hansecom.monitoringservice.service.dto.PageRequestDto;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** The controller class that handles REST requests for {@link JobsExecutionApi}. */
@RestController
@RequiredArgsConstructor
public class JobExecutionController implements JobsExecutionApi {

  private final ModelMapper mapper;

  private final JobExecutionService jobExecutionService;

  @Override
  public ResponseEntity<JobExecutionResponseList> getJobsExecutions(
      String jobName,
      String url,
      String status,
      Instant from,
      Instant to,
      Integer offset,
      Integer limit,
      String sortDirection,
      Set<String> sortProperties) {

    JobExecutionDtoList jobExecutionDtoList =
        jobExecutionService.getJobsExecutions(
            JobExecutionFilterDto.builder()
                .jobName(Optional.ofNullable(jobName))
                .url(Optional.ofNullable(url))
                .status(Optional.ofNullable(status).map(JobExecutionStatus::fromString))
                .from(Optional.ofNullable(from))
                .to(Optional.ofNullable(to))
                .pageRequestDto(
                    Optional.of(
                        new PageRequestDto(
                            offset,
                            limit,
                            PageRequestDto.SortDirection.fromString(sortDirection),
                            sortProperties)))
                .build());

    return ResponseEntity.ok(mapper.map(jobExecutionDtoList, JobExecutionResponseList.class));
  }
}
