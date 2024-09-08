package com.hansecom.monitoringservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.hansecom.monitoringservice.configuration.ModelMapperConfiguration;
import com.hansecom.monitoringservice.exception.StandardException;
import com.hansecom.monitoringservice.job.UrlMonitorJob;
import com.hansecom.monitoringservice.service.dto.JobDto;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;

@SpringBootTest(
    classes = {
      JobServiceImpl.class,
      ValidationAutoConfiguration.class,
      ModelMapperConfiguration.class
    })
class JobServiceImplTest {

  @Autowired private JobServiceImpl testSubject;

  @MockBean private Scheduler schedulerMock;

  @Test
  void createJob_invalidInput_throwConstraintViolationException() {

    assertThatThrownBy(() -> testSubject.createJob(null))
        .isInstanceOf(ConstraintViolationException.class)
        .hasMessage("createJob.jobDto: must not be null");

    JobDto jobDtoAllNull = new JobDto(null, null, null, null, null);

    assertThatThrownBy(() -> testSubject.createJob(jobDtoAllNull))
        .isInstanceOf(ConstraintViolationException.class)
        .hasMessageContaining("createJob.jobDto.name: name must not be blank")
        .hasMessageContaining("createJob.jobDto.description: description must not be blank")
        .hasMessageContaining("createJob.jobDto.url: url must not be blank")
        .hasMessageContaining(
            "createJob.jobDto.cronExpression: cronExpression not a valid quartz cron expression");

    JobDto jobDtoInvalid = new JobDto(" ", " ", "invalid-url", "inlavis-cron", "invalid-timezone");

    assertThatThrownBy(() -> testSubject.createJob(jobDtoInvalid))
        .isInstanceOf(ConstraintViolationException.class)
        .hasMessageContaining("createJob.jobDto.name: name must not be blank")
        .hasMessageContaining("createJob.jobDto.description: description must not be blank")
        .hasMessageContaining("createJob.jobDto.url: url must be a valid URL")
        .hasMessageContaining(
            "createJob.jobDto.cronExpression: cronExpression not a valid quartz cron expression")
        .hasMessageContaining("createJob.jobDto.timezone: timezone invalid timezone ID");

    verifyNoInteractions(schedulerMock);
  }

  @SneakyThrows
  @Test
  void createJob_exceedMaxJobsAllowed_throwSEMaxJobsAllowed() {

    JobDto jobDto =
        JobDto.builder()
            .name("google")
            .description("check server up")
            .url("https://www.google.com")
            .cronExpression("0/10 * * ? * * *")
            .timezone("Europe/Lisbon")
            .build();

    when(schedulerMock.getJobKeys(GroupMatcher.anyGroup()))
        .thenReturn(
            Set.of(
                JobKey.jobKey("1"),
                JobKey.jobKey("2"),
                JobKey.jobKey("3"),
                JobKey.jobKey("4"),
                JobKey.jobKey("5")));

    String errorMessage = "You have reach the system limit of 5 jobs";

    assertThatThrownBy(() -> testSubject.createJob(jobDto))
        .hasMessage(errorMessage)
        .isInstanceOfSatisfying(
            StandardException.class,
            e -> {
              assertThat(e.isTech()).isFalse();
              assertThat(e.getErrorType()).isEqualTo(StandardException.ErrorType.FUNC);
              assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
              assertThat(e.getProblemDetail().getStatus()).isEqualTo(422);
              assertThat(e.getProblemDetail().getTitle()).isEqualTo("Max jobs reached");
              assertThat(e.getProblemDetail().getCode()).isEqualTo("max_jobs_reach");
              assertThat(e.getProblemDetail().getDetail()).isEqualTo(errorMessage);
            });

    verify(schedulerMock).getJobKeys(GroupMatcher.anyGroup());
    verifyNoMoreInteractions(schedulerMock);
  }

  @SneakyThrows
  @Test
  void createJob_jobNameAlreadyExists_throwSEJobNameAlreadyExists() {

    JobDto jobDto =
        JobDto.builder()
            .name("google")
            .description("check server up")
            .url("https://www.google.com")
            .cronExpression("0/10 * * ? * * *")
            .timezone("Europe/Lisbon")
            .build();

    when(schedulerMock.getJobKeys(GroupMatcher.anyGroup())).thenReturn(Set.of());
    when(schedulerMock.checkExists(JobKey.jobKey(jobDto.getName()))).thenReturn(true);

    String errorMessage = "Job with name google already exists";

    assertThatThrownBy(() -> testSubject.createJob(jobDto))
        .hasMessage(errorMessage)
        .isInstanceOfSatisfying(
            StandardException.class,
            e -> {
              assertThat(e.isTech()).isFalse();
              assertThat(e.getErrorType()).isEqualTo(StandardException.ErrorType.FUNC);
              assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
              assertThat(e.getProblemDetail().getStatus()).isEqualTo(409);
              assertThat(e.getProblemDetail().getTitle()).isEqualTo("Job already exists");
              assertThat(e.getProblemDetail().getCode()).isEqualTo("job_already_exists");
              assertThat(e.getProblemDetail().getDetail()).isEqualTo(errorMessage);
              assertThat(e.getProblemDetail().getAdditionalProperties()).hasSize(1);
              assertThat(e.getProblemDetail().getAdditionalProperty("jobName")).isEqualTo("google");
            });

    verify(schedulerMock).getJobKeys(GroupMatcher.anyGroup());
    verify(schedulerMock).checkExists(JobKey.jobKey(jobDto.getName()));
    verifyNoMoreInteractions(schedulerMock);
  }

  @SneakyThrows
  @Test
  void createJob_noErrors_succeed() {

    JobDto jobDto =
        JobDto.builder()
            .name("google")
            .description("check server up")
            .url("https://www.google.com")
            .cronExpression("0/10 * * ? * * *")
            .timezone("Europe/Lisbon")
            .build();

    when(schedulerMock.getJobKeys(GroupMatcher.anyGroup())).thenReturn(Set.of());
    when(schedulerMock.checkExists(JobKey.jobKey(jobDto.getName()))).thenReturn(false);

    testSubject.createJob(jobDto);

    verify(schedulerMock).getJobKeys(GroupMatcher.anyGroup());
    verify(schedulerMock).checkExists(JobKey.jobKey(jobDto.getName()));

    verify(schedulerMock)
        .scheduleJob(
            assertArg(
                jobDetail -> {
                  assertThat(jobDetail.getKey().getName()).isEqualTo("google");
                  assertThat(jobDetail.getJobClass()).isEqualTo(UrlMonitorJob.class);
                  assertThat(jobDetail.getDescription()).isEqualTo(jobDto.getDescription());
                  assertThat(jobDetail.getJobDataMap().get("url")).isEqualTo(jobDto.getUrl());
                }),
            assertArg(
                trigger -> {
                  assertThat(trigger.getKey().getName()).isEqualTo("Trigger for google");
                  assertThat(trigger.getDescription())
                      .isEqualTo("Trigger for " + jobDto.getDescription());
                }));

    verifyNoMoreInteractions(schedulerMock);
  }
}
