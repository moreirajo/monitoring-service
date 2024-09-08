package com.hansecom.monitoringservice.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.hansecom.monitoringservice.configuration.ModelMapperConfiguration;
import com.hansecom.monitoringservice.persistence.model.JobExecutionStatusEntity;
import com.hansecom.monitoringservice.persistence.repository.JobExecutionRepository;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

@SpringBootTest(
    classes = {
      UrlMonitorJob.class,
      ValidationAutoConfiguration.class,
      ModelMapperConfiguration.class
    })
class UrlMonitorJobTest {

  @Autowired private UrlMonitorJob testSubject;

  @MockBean private JobExecutionRepository jobExecutionRepositoryMock;

  @MockBean private JobExecutionContext jobExecutionContextMock;

  public static MockWebServer mockWebClient = new MockWebServer();

  @SneakyThrows
  @BeforeAll
  static void setUp() {
    mockWebClient.start();
  }

  @SneakyThrows
  @AfterAll
  static void tearDown() {
    mockWebClient.shutdown();
  }

  @BeforeEach
  void jobDataMap() {

    JobDetail jobDetail =
        JobBuilder.newJob(UrlMonitorJob.class)
            .withIdentity("google")
            .withDescription("google test")
            .usingJobData("url", "http://localhost:" + mockWebClient.getPort())
            .build();

    when(jobExecutionContextMock.getJobDetail()).thenReturn(jobDetail);
  }

  @SneakyThrows
  @Test
  void execute_responseSuccess_saveWithStatusSucceed() {

    mockWebClient.enqueue(
        new MockResponse()
            .setResponseCode(HttpStatus.OK.value())
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("all good"));

    testSubject.execute(jobExecutionContextMock);

    verify(jobExecutionRepositoryMock)
        .save(
            assertArg(
                jobExecutionEntity -> {
                  assertThat(jobExecutionEntity.getJobName()).isEqualTo("google");
                  assertThat(jobExecutionEntity.getUrl())
                      .isEqualTo("http://localhost:" + mockWebClient.getPort());
                  assertThat(jobExecutionEntity.getStatus())
                      .isEqualTo(JobExecutionStatusEntity.SUCCEEDED);
                  assertThat(jobExecutionEntity.getResponseTime()).isCloseTo(1000L, within(30000L));
                }));

    verifyNoMoreInteractions(jobExecutionRepositoryMock);

    verify(jobExecutionContextMock, times(2)).getJobDetail();
    verifyNoMoreInteractions(jobExecutionContextMock);
  }

  @Test
  void execute_responseError_saveWithStatusFailed() {

    mockWebClient.enqueue(
        new MockResponse()
            .setResponseCode(HttpStatus.NOT_FOUND.value())
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    try {
      testSubject.execute(jobExecutionContextMock);
    } catch (Exception e) {
    }

    verify(jobExecutionRepositoryMock)
        .save(
            assertArg(
                jobExecutionEntity -> {
                  assertThat(jobExecutionEntity.getJobName()).isEqualTo("google");
                  assertThat(jobExecutionEntity.getUrl())
                      .isEqualTo("http://localhost:" + mockWebClient.getPort());
                  assertThat(jobExecutionEntity.getStatus())
                      .isEqualTo(JobExecutionStatusEntity.FAILED);
                  assertThat(jobExecutionEntity.getErrorMessage())
                      .isEqualTo(
                          "404 Not Found from GET http://localhost:" + mockWebClient.getPort());
                  assertThat(jobExecutionEntity.getResponseTime()).isCloseTo(1000L, within(1000L));
                }));

    verifyNoMoreInteractions(jobExecutionRepositoryMock);

    verify(jobExecutionContextMock, times(2)).getJobDetail();
    verifyNoMoreInteractions(jobExecutionContextMock);
  }
}
