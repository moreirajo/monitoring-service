package com.hansecom.monitoringservice.rest.inbound.controller;

import com.hansecom.monitoringservice.rest.inbound.openapi.api.JobsManagementApi;
import com.hansecom.monitoringservice.rest.inbound.openapi.model.Job;
import com.hansecom.monitoringservice.service.JobService;
import com.hansecom.monitoringservice.service.dto.JobDto;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** The controller class that handles REST requests for {@link JobsManagementApi}. */
@RestController
@RequiredArgsConstructor
public class JobManagementController implements JobsManagementApi {

  private final ModelMapper mapper;

  private final JobService jobService;

  @Override
  public ResponseEntity<Job> createJob(Job job) {

    JobDto jobDto = jobService.createJob(mapper.map(job, JobDto.class));

    return ResponseEntity.ok(mapper.map(jobDto, Job.class));
  }
}
