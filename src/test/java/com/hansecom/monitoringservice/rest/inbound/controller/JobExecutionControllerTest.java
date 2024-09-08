package com.hansecom.monitoringservice.rest.inbound.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.hansecom.monitoringservice.configuration.ModelMapperConfiguration;
import com.hansecom.monitoringservice.exception.StandardException;
import com.hansecom.monitoringservice.rest.inbound.openapi.model.JobExecutionResponseList;
import com.hansecom.monitoringservice.rest.inbound.openapi.model.ProblemDetail;
import com.hansecom.monitoringservice.service.JobExecutionService;
import com.hansecom.monitoringservice.service.dto.JobExecutionDto;
import com.hansecom.monitoringservice.service.dto.JobExecutionDtoList;
import com.hansecom.monitoringservice.service.dto.JobExecutionFilterDto;
import com.hansecom.monitoringservice.service.dto.JobExecutionStatus;
import com.hansecom.monitoringservice.service.dto.PageRequestDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.ErrorResponseException;

@Import({ValidationAutoConfiguration.class, ModelMapperConfiguration.class})
@WebMvcTest(JobExecutionController.class)
class JobExecutionControllerTest {

  private static final String BASE_PATH = "/jobs-executions";

  @Autowired private WebTestClient webTestClient;

  @MockBean private JobExecutionService jobExecutionServiceMock;

  @ParameterizedTest
  @MethodSource("getJobsExecutionsInvalidParams")
  void getJobsExecutions_wrongQueryParamValue_failWith400(
      final String paramName, final String errorDetail) {

    webTestClient
        .get()
        .uri(uriBuilder -> uriBuilder.path(BASE_PATH).queryParam(paramName, "some-string").build())
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody(ProblemDetail.class)
        .value(
            problemDetail -> {
              String code = "invalid_request_params";
              assertErrorResponseDefaults(problemDetail, true, code);
              assertThat(problemDetail.getStatus()).isEqualTo(400);
              assertThat(problemDetail.getTitle())
                  .isEqualTo("Your request parameters didn't validate");
              assertThat(problemDetail.getCode()).isEqualTo(code);
              assertThat(problemDetail.getDetail()).isEqualTo(errorDetail);
              assertThat(problemDetail.getInvalidParams()).hasSize(1);
              assertThat(problemDetail.getInvalidParams().getFirst().getName())
                  .isEqualTo(paramName);
              assertThat(problemDetail.getInvalidParams().getFirst().getReason())
                  .isEqualTo(errorDetail);
            });

    verifyNoInteractions(jobExecutionServiceMock);
  }

  @Test
  void getJobsExecutions_validationErrorFromService_failWith400() {

    ConstraintViolationException constraintViolationExceptionMock =
        mock(ConstraintViolationException.class);

    ConstraintViolation<?> constraintViolationMock = mock(ConstraintViolation.class);

    when(constraintViolationExceptionMock.getConstraintViolations())
        .thenReturn(Set.of(constraintViolationMock));

    when(constraintViolationExceptionMock.getMessage()).thenReturn("Field test");

    when(constraintViolationMock.getPropertyPath())
        .thenReturn(PathImpl.createPathFromString("jobName"));

    when(constraintViolationMock.getMessage()).thenReturn("unit test");

    JobExecutionFilterDto jobExecutionFilterDto =
        new JobExecutionFilterDto(
            Optional.of("google"),
            Optional.of("https://www.google.com"),
            Optional.of(JobExecutionStatus.SUCCEEDED),
            Optional.empty(),
            Optional.empty(),
            Optional.of(
                new PageRequestDto(
                    0, 10, PageRequestDto.SortDirection.ASC, Set.of("createdDate"))));

    when(jobExecutionServiceMock.getJobsExecutions(jobExecutionFilterDto))
        .thenThrow(constraintViolationExceptionMock);

    webTestClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path(BASE_PATH)
                    .queryParam("jobName", "google")
                    .queryParam("url", "https://www.google.com")
                    .queryParam("status", "SUCCEEDED")
                    .queryParam("offset", 0)
                    .queryParam("limit", 10)
                    .queryParam("sort-direction", "asc")
                    .queryParam("sort-properties", "createdDate")
                    .build())
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody(ProblemDetail.class)
        .value(
            problemDetail -> {
              String code = "invalid_request_params";
              assertErrorResponseDefaults(problemDetail, true, code);
              assertThat(problemDetail.getStatus()).isEqualTo(400);
              assertThat(problemDetail.getTitle())
                  .isEqualTo("Your request parameters didn't validate");
              assertThat(problemDetail.getCode()).isEqualTo(code);
              assertThat(problemDetail.getDetail()).isEqualTo("Field test");
              assertThat(problemDetail.getInvalidParams()).hasSize(1);
              assertThat(problemDetail.getInvalidParams().getFirst().getName())
                  .isEqualTo("jobName");
              assertThat(problemDetail.getInvalidParams().getFirst().getReason())
                  .isEqualTo("unit test");
            });

    verify(jobExecutionServiceMock).getJobsExecutions(jobExecutionFilterDto);
    verifyNoMoreInteractions(jobExecutionServiceMock);
  }

  @Test
  void getJobsExecutions_standardExceptionFromService_failWith409() {

    JobExecutionFilterDto jobExecutionFilterDto =
        new JobExecutionFilterDto(
            Optional.of("google"),
            Optional.of("https://www.google.com"),
            Optional.of(JobExecutionStatus.SUCCEEDED),
            Optional.empty(),
            Optional.empty(),
            Optional.of(
                new PageRequestDto(
                    0, 10, PageRequestDto.SortDirection.ASC, Set.of("createdDate"))));

    String code = "test_409";

    when(jobExecutionServiceMock.getJobsExecutions(jobExecutionFilterDto))
        .thenThrow(
            new StandardException(
                "Unit test 409",
                new ProblemDetail()
                    .status(409)
                    .title("Unit test")
                    .detail("Unit test 409")
                    .code(code)
                    .type("https://www.hansecom.com/errors/" + code)
                    .traceId("00000000000000000000000000000000"),
                StandardException.ErrorType.FUNC,
                HttpStatus.CONFLICT));

    webTestClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path(BASE_PATH)
                    .queryParam("jobName", "google")
                    .queryParam("url", "https://www.google.com")
                    .queryParam("status", "SUCCEEDED")
                    .queryParam("offset", 0)
                    .queryParam("limit", 10)
                    .queryParam("sort-direction", "asc")
                    .queryParam("sort-properties", "createdDate")
                    .build())
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.CONFLICT)
        .expectBody(ProblemDetail.class)
        .value(
            problemDetail -> {
              assertErrorResponseDefaults(problemDetail, false, code);
              assertThat(problemDetail.getStatus()).isEqualTo(409);
              assertThat(problemDetail.getTitle()).isEqualTo("Unit test");
              assertThat(problemDetail.getCode()).isEqualTo(code);
              assertThat(problemDetail.getDetail()).isEqualTo("Unit test 409");
            });

    verify(jobExecutionServiceMock).getJobsExecutions(jobExecutionFilterDto);
    verifyNoMoreInteractions(jobExecutionServiceMock);
  }

  @Test
  void getJobsExecutions_errorFromService_failWith400() {

    JobExecutionFilterDto jobExecutionFilterDto =
        new JobExecutionFilterDto(
            Optional.of("google"),
            Optional.of("https://www.google.com"),
            Optional.of(JobExecutionStatus.SUCCEEDED),
            Optional.empty(),
            Optional.empty(),
            Optional.of(
                new PageRequestDto(
                    0, 10, PageRequestDto.SortDirection.ASC, Set.of("createdDate"))));

    when(jobExecutionServiceMock.getJobsExecutions(jobExecutionFilterDto))
        .thenThrow(new ErrorResponseException(HttpStatus.BAD_REQUEST));

    webTestClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path(BASE_PATH)
                    .queryParam("jobName", "google")
                    .queryParam("url", "https://www.google.com")
                    .queryParam("status", "SUCCEEDED")
                    .queryParam("offset", 0)
                    .queryParam("limit", 10)
                    .queryParam("sort-direction", "asc")
                    .queryParam("sort-properties", "createdDate")
                    .build())
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody(ProblemDetail.class)
        .value(
            problemDetail -> {
              String code = "problem_with_request";
              assertErrorResponseDefaults(problemDetail, false, code);
              assertThat(problemDetail.getStatus()).isEqualTo(400);
              assertThat(problemDetail.getTitle()).isEqualTo("There is a problem with the request");
              assertThat(problemDetail.getCode()).isEqualTo(code);
              assertThat(problemDetail.getDetail())
                  .isEqualTo(
                      "400 BAD_REQUEST, ProblemDetail[type='about:blank', title='Bad Request', status=400, detail='null', instance='null', properties='null']");
            });

    verify(jobExecutionServiceMock).getJobsExecutions(jobExecutionFilterDto);
    verifyNoMoreInteractions(jobExecutionServiceMock);
  }

  @ParameterizedTest
  @MethodSource("jobExecutionServiceThrownException")
  void getJobsExecutions_errorFromServiceTech_failWith500(final Exception thrownEx) {

    JobExecutionFilterDto jobExecutionFilterDto =
        new JobExecutionFilterDto(
            Optional.of("google"),
            Optional.of("https://www.google.com"),
            Optional.of(JobExecutionStatus.SUCCEEDED),
            Optional.empty(),
            Optional.empty(),
            Optional.of(
                new PageRequestDto(
                    0, 10, PageRequestDto.SortDirection.ASC, Set.of("createdDate"))));

    when(jobExecutionServiceMock.getJobsExecutions(jobExecutionFilterDto)).thenThrow(thrownEx);

    webTestClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path(BASE_PATH)
                    .queryParam("jobName", "google")
                    .queryParam("url", "https://www.google.com")
                    .queryParam("status", "SUCCEEDED")
                    .queryParam("offset", 0)
                    .queryParam("limit", 10)
                    .queryParam("sort-direction", "asc")
                    .queryParam("sort-properties", "createdDate")
                    .build())
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        .expectBody(ProblemDetail.class)
        .value(
            problemDetail -> {
              String code = "internal_server_error";
              assertErrorResponseDefaults(problemDetail, false, code);
              assertThat(problemDetail.getStatus()).isEqualTo(500);
              assertThat(problemDetail.getTitle()).isEqualTo("Internal error on the server");
              assertThat(problemDetail.getCode()).isEqualTo(code);
              assertThat(problemDetail.getDetail())
                  .isEqualTo(
                      "Some internal server error happen. Please provide the traceId 00000000000000000000000000000000 to the support team.");
            });

    verify(jobExecutionServiceMock).getJobsExecutions(jobExecutionFilterDto);
    verifyNoMoreInteractions(jobExecutionServiceMock);
  }

  @Test
  void getJobsExecutions_noErrors_succeed() {

    JobExecutionFilterDto jobExecutionFilterDto =
        new JobExecutionFilterDto(
            Optional.of("google"),
            Optional.of("https://www.google.com"),
            Optional.of(JobExecutionStatus.FAILED),
            Optional.empty(),
            Optional.empty(),
            Optional.of(
                new PageRequestDto(
                    0, 10, PageRequestDto.SortDirection.ASC, Set.of("createdDate"))));

    JobExecutionDto jobExecutionDto =
        JobExecutionDto.builder()
            .jobName("google")
            .url("https://www.google.com")
            .createdDate(Instant.now())
            .responseTime(80L)
            .status(JobExecutionStatus.FAILED)
            .errorMessage("404 not found")
            .build();

    when(jobExecutionServiceMock.getJobsExecutions(jobExecutionFilterDto))
        .thenReturn(
            JobExecutionDtoList.builder()
                .jobExecutionList(List.of(jobExecutionDto))
                .totalPages(1)
                .totalElements(1)
                .build());

    webTestClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path(BASE_PATH)
                    .queryParam("jobName", "google")
                    .queryParam("url", "https://www.google.com")
                    .queryParam("status", "FAILED")
                    .queryParam("offset", 0)
                    .queryParam("limit", 10)
                    .queryParam("sort-direction", "asc")
                    .queryParam("sort-properties", "createdDate")
                    .build())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(JobExecutionResponseList.class)
        .value(
            jobExecutionResponseList -> {
              assertThat(jobExecutionResponseList.getJobExecutionList()).hasSize(1);
              assertThat(jobExecutionResponseList.getJobExecutionList().getFirst())
                  .usingRecursiveComparison()
                  .ignoringFields("responseTime")
                  .isEqualTo(jobExecutionDto);
              assertThat(
                      jobExecutionResponseList.getJobExecutionList().getFirst().getResponseTime())
                  .isEqualTo(80);
              assertThat(jobExecutionResponseList.getTotalPages()).isOne();
              assertThat(jobExecutionResponseList.getTotalElements()).isOne();
            });

    verify(jobExecutionServiceMock).getJobsExecutions(jobExecutionFilterDto);
    verifyNoMoreInteractions(jobExecutionServiceMock);
  }

  private void assertErrorResponseDefaults(
      final ProblemDetail problemDetail, boolean hasInvalidParams, String code) {

    assertThat(problemDetail.getType()).isEqualTo("https://www.hansecom.com/errors/" + code);
    if (!hasInvalidParams) {
      assertThat(problemDetail.getInvalidParams()).isNull();
    }
    assertThat(problemDetail.getTraceId()).isEqualTo("00000000000000000000000000000000");
  }

  private static Stream<Arguments> getJobsExecutionsInvalidParams() {
    // paramName, errorDetail

    return Stream.of(
        Arguments.of(
            "offset",
            "Failed to convert value of type 'java.lang.String' to required type 'java.lang.Integer'; For input string: \"some-string\""),
        Arguments.of(
            "limit",
            "Failed to convert value of type 'java.lang.String' to required type 'java.lang.Integer'; For input string: \"some-string\""),
        Arguments.of(
            "sort-direction", "Invalid sort direction value. Please provide one of [ASC, DESC]"));
  }

  private static Stream<Arguments> jobExecutionServiceThrownException() {
    return Stream.of(
        Arguments.of(StandardException.internalServerError(new NullPointerException("unit test"))),
        Arguments.of(new NullPointerException("unit test")));
  }
}
