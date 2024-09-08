package com.hansecom.monitoringservice.rest.inbound.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.hansecom.monitoringservice.configuration.ModelMapperConfiguration;
import com.hansecom.monitoringservice.exception.StandardException;
import com.hansecom.monitoringservice.rest.inbound.openapi.model.Job;
import com.hansecom.monitoringservice.rest.inbound.openapi.model.ProblemDetail;
import com.hansecom.monitoringservice.service.JobService;
import com.hansecom.monitoringservice.service.dto.JobDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
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
@WebMvcTest(JobManagementController.class)
class JobManagementControllerTest {

  private static final String BASE_PATH = "/jobs";

  @Autowired private WebTestClient webTestClient;

  @MockBean private JobService jobServiceMock;

  @Test
  void createJob_validationErrorFromService_failWith400() {

    JobDto jobDto =
        JobDto.builder()
            .name("google")
            .url("https://www.google.com")
            .description("google test")
            .cronExpression("0/10 * * ? * * *")
            .timezone("Europe/Lisbon")
            .build();

    ConstraintViolationException constraintViolationExceptionMock =
        mock(ConstraintViolationException.class);

    ConstraintViolation<?> constraintViolationMock = mock(ConstraintViolation.class);

    when(constraintViolationExceptionMock.getConstraintViolations())
        .thenReturn(Set.of(constraintViolationMock));

    when(constraintViolationExceptionMock.getMessage()).thenReturn("methodName: Field test");

    when(constraintViolationMock.getPropertyPath())
        .thenReturn(PathImpl.createPathFromString("name"));

    when(constraintViolationMock.getMessage()).thenReturn("unit test");

    when(jobServiceMock.createJob(jobDto)).thenThrow(constraintViolationExceptionMock);

    Job job =
        new Job()
            .name("google")
            .url("https://www.google.com")
            .description("google test")
            .cronExpression("0/10 * * ? * * *")
            .timezone("Europe/Lisbon");

    webTestClient
        .post()
        .uri(BASE_PATH)
        .bodyValue(job)
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
              assertThat(problemDetail.getInvalidParams().getFirst().getName()).isEqualTo("name");
              assertThat(problemDetail.getInvalidParams().getFirst().getReason())
                  .isEqualTo("unit test");
            });

    verify(jobServiceMock).createJob(jobDto);
    verifyNoMoreInteractions(jobServiceMock);
  }

  @Test
  void createJob_standardExceptionFromService_failWith409() {

    JobDto jobDto =
        JobDto.builder()
            .name("google")
            .url("https://www.google.com")
            .description("google test")
            .cronExpression("0/10 * * ? * * *")
            .timezone("Europe/Lisbon")
            .build();

    String code = "test_409";

    when(jobServiceMock.createJob(jobDto))
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

    Job job =
        new Job()
            .name("google")
            .url("https://www.google.com")
            .description("google test")
            .cronExpression("0/10 * * ? * * *")
            .timezone("Europe/Lisbon");

    webTestClient
        .post()
        .uri(BASE_PATH)
        .bodyValue(job)
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

    verify(jobServiceMock).createJob(jobDto);
    verifyNoMoreInteractions(jobServiceMock);
  }

  @Test
  void createJob_errorFromService_failWith400() {

    JobDto jobDto =
        JobDto.builder()
            .name("google")
            .url("https://www.google.com")
            .description("google test")
            .cronExpression("0/10 * * ? * * *")
            .timezone("Europe/Lisbon")
            .build();

    when(jobServiceMock.createJob(jobDto))
        .thenThrow(new ErrorResponseException(HttpStatus.BAD_REQUEST));

    Job job =
        new Job()
            .name("google")
            .url("https://www.google.com")
            .description("google test")
            .cronExpression("0/10 * * ? * * *")
            .timezone("Europe/Lisbon");

    webTestClient
        .post()
        .uri(BASE_PATH)
        .bodyValue(job)
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

    verify(jobServiceMock).createJob(jobDto);
    verifyNoMoreInteractions(jobServiceMock);
  }

  @ParameterizedTest
  @MethodSource("jobServiceThrownException")
  void createJob_errorFromServiceTech_failWith500(final Exception thrownEx) {

    JobDto jobDto =
        JobDto.builder()
            .name("google")
            .url("https://www.google.com")
            .description("google test")
            .cronExpression("0/10 * * ? * * *")
            .timezone("Europe/Lisbon")
            .build();

    when(jobServiceMock.createJob(jobDto)).thenThrow(thrownEx);

    Job job =
        new Job()
            .name("google")
            .url("https://www.google.com")
            .description("google test")
            .cronExpression("0/10 * * ? * * *")
            .timezone("Europe/Lisbon");

    webTestClient
        .post()
        .uri(BASE_PATH)
        .bodyValue(job)
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

    verify(jobServiceMock).createJob(jobDto);
    verifyNoMoreInteractions(jobServiceMock);
  }

  @Test
  void createJob_noErrors_succeed() {

    JobDto jobDto =
        JobDto.builder()
            .name("google")
            .url("https://www.google.com")
            .description("google test")
            .cronExpression("0/10 * * ? * * *")
            .timezone("Europe/Lisbon")
            .build();

    when(jobServiceMock.createJob(jobDto)).thenReturn(jobDto);

    Job job =
        new Job()
            .name("google")
            .url("https://www.google.com")
            .description("google test")
            .cronExpression("0/10 * * ? * * *")
            .timezone("Europe/Lisbon");

    webTestClient
        .post()
        .uri(BASE_PATH)
        .bodyValue(job)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(Job.class)
        .value(jobResponse -> assertThat(jobResponse).usingRecursiveComparison().isEqualTo(jobDto));

    verify(jobServiceMock).createJob(jobDto);
    verifyNoMoreInteractions(jobServiceMock);
  }

  private void assertErrorResponseDefaults(
      final ProblemDetail problemDetail, boolean hasInvalidParams, String code) {

    assertThat(problemDetail.getType()).isEqualTo("https://www.hansecom.com/errors/" + code);
    if (!hasInvalidParams) {
      assertThat(problemDetail.getInvalidParams()).isNull();
    }
    assertThat(problemDetail.getTraceId()).isEqualTo("00000000000000000000000000000000");
  }

  private static Stream<Arguments> jobServiceThrownException() {
    return Stream.of(
        Arguments.of(StandardException.internalServerError(new NullPointerException("unit test"))),
        Arguments.of(new NullPointerException("unit test")));
  }
}
