package com.hansecom.monitoringservice.service.impl;

import com.hansecom.monitoringservice.exception.StandardException;
import com.hansecom.monitoringservice.job.UrlMonitorJob;
import com.hansecom.monitoringservice.service.JobService;
import com.hansecom.monitoringservice.service.dto.JobDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Implementation service of {@link JobService}. */
@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

  private final Lock jobValidationLock = new ReentrantLock();

  private final Scheduler scheduler;

  @Value("${custom-properties.max-jobs-allowed:5}")
  private int maxJobsAllowed;

  @SneakyThrows
  @Override
  public JobDto createJob(@NotNull @Valid final JobDto jobDto) {

    validations(jobDto);

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

  /**
   * Performs validations on the job before it is scheduled. This method checks if the maximum
   * number of allowed jobs has been reached and if a job with the given name already exists.
   *
   * <p>This method is thread-safe. It uses a ReentrantLock to ensure that only one thread can
   * perform validations at a time. This prevents race conditions when multiple threads attempt to
   * create jobs simultaneously, maintaining the integrity of the job count and uniqueness of job
   * names.
   *
   * @param jobDto the JobDto object containing the details of the job to be validated
   * @throws SchedulerException if there's an error while interacting with the scheduler.
   * @throws StandardException if the maximum number of allowed jobs has been reached or if a job
   *     with the given name already exists
   */
  private void validations(JobDto jobDto) throws SchedulerException {
    jobValidationLock.lock();
    try {
      if (scheduler.getJobKeys(GroupMatcher.anyGroup()).size() >= maxJobsAllowed) {
        throw StandardException.maxJobsAllowed(maxJobsAllowed);
      }

      if (scheduler.checkExists(JobKey.jobKey(jobDto.getName()))) {
        throw StandardException.jobNameAlreadyExists(jobDto.getName());
      }
    } finally {
      jobValidationLock.unlock();
    }
  }
}
