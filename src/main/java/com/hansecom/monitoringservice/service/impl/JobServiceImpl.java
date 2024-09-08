package com.hansecom.monitoringservice.service.impl;

import com.hansecom.monitoringservice.exception.StandardException;
import com.hansecom.monitoringservice.job.UrlMonitorJob;
import com.hansecom.monitoringservice.service.JobService;
import com.hansecom.monitoringservice.service.dto.JobDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import java.util.TimeZone;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Implementation service of {@link JobService}. */
@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

  private final Scheduler scheduler;

  @Value("${custom-properties.max-jobs-allowed:5}")
  private int maxJobsAllowed;

  @SneakyThrows
  @Override
  public JobDto createJob(@NotNull @Valid final JobDto jobDto) {

    if (scheduler.getJobKeys(GroupMatcher.anyGroup()).size() >= maxJobsAllowed) {
      throw StandardException.maxJobsAllowed(maxJobsAllowed);
    }

    if (scheduler.checkExists(JobKey.jobKey(jobDto.getName()))) {
      throw StandardException.jobNameAlreadyExists(jobDto.getName());
    }

    JobDetail jobDetail =
        JobBuilder.newJob(UrlMonitorJob.class)
            .withIdentity(jobDto.getName())
            .withDescription(jobDto.getDescription())
            .usingJobData("url", jobDto.getUrl())
            .build();

    CronTrigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity("Trigger for " + jobDto.getName())
            .withDescription("Trigger for " + jobDto.getDescription())
            .withSchedule(
                CronScheduleBuilder.cronSchedule(jobDto.getCronExpression())
                    .inTimeZone(
                        TimeZone.getTimeZone(
                            Optional.ofNullable(jobDto.getTimezone()).orElse("UTC"))))
            .build();

    scheduler.scheduleJob(jobDetail, trigger);

    return jobDto;
  }
}
