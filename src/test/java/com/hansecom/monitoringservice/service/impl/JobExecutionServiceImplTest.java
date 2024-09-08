package com.hansecom.monitoringservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.hansecom.monitoringservice.configuration.ModelMapperConfiguration;
import com.hansecom.monitoringservice.exception.StandardException;
import com.hansecom.monitoringservice.persistence.model.JobExecutionCriteriaParams;
import com.hansecom.monitoringservice.persistence.model.JobExecutionEntity;
import com.hansecom.monitoringservice.persistence.model.JobExecutionStatusEntity;
import com.hansecom.monitoringservice.persistence.repository.JobExecutionRepository;
import com.hansecom.monitoringservice.service.dto.JobExecutionDtoList;
import com.hansecom.monitoringservice.service.dto.JobExecutionFilterDto;
import com.hansecom.monitoringservice.service.dto.JobExecutionStatus;
import com.hansecom.monitoringservice.service.dto.PageRequestDto;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

@SpringBootTest(
    classes = {
      JobExecutionServiceImpl.class,
      ValidationAutoConfiguration.class,
      ModelMapperConfiguration.class
    })
class JobExecutionServiceImplTest {

  @Autowired private JobExecutionServiceImpl testSubject;

  @MockBean private JobExecutionRepository jobExecutionRepositoryMock;

  @Test
  void getJobsExecutions_invalidInput_throwConstraintViolationException() {

    assertThatThrownBy(() -> testSubject.getJobsExecutions(null))
        .isInstanceOf(ConstraintViolationException.class)
        .hasMessage("getJobsExecutions.jobExecutionFilterDto: must not be null");

    JobExecutionFilterDto jobExecutionFilterDtoAllNull =
        new JobExecutionFilterDto(null, null, null, null, null, null);

    assertThatThrownBy(() -> testSubject.getJobsExecutions(jobExecutionFilterDtoAllNull))
        .isInstanceOf(ConstraintViolationException.class)
        .hasMessageContaining("getJobsExecutions.jobExecutionFilterDto.jobName: must not be null")
        .hasMessageContaining("getJobsExecutions.jobExecutionFilterDto.url: must not be null")
        .hasMessageContaining("getJobsExecutions.jobExecutionFilterDto.status: must not be null")
        .hasMessageContaining("getJobsExecutions.jobExecutionFilterDto.from: must not be null")
        .hasMessageContaining("getJobsExecutions.jobExecutionFilterDto.to: must not be null")
        .hasMessageContaining(
            "getJobsExecutions.jobExecutionFilterDto.pageRequestDto: must not be null");

    Optional<PageRequestDto> pageRequestDto = Optional.of(new PageRequestDto(-1, 0, null, null));
    JobExecutionFilterDto jobExecutionFilterDtoInvalidPage =
        new JobExecutionFilterDto(
            Optional.of("google"),
            Optional.of("invalid-url"),
            Optional.of(JobExecutionStatus.SUCCEEDED),
            Optional.empty(),
            Optional.empty(),
            pageRequestDto);

    assertThatThrownBy(() -> testSubject.getJobsExecutions(jobExecutionFilterDtoInvalidPage))
        .isInstanceOf(ConstraintViolationException.class)
        .hasMessageContaining(
            "getJobsExecutions.jobExecutionFilterDto.url: url must be a valid URL")
        .hasMessageContaining(
            "getJobsExecutions.jobExecutionFilterDto.pageRequestDto.pageSize: page size must be greater than 0")
        .hasMessageContaining(
            "getJobsExecutions.jobExecutionFilterDto.pageRequestDto.pageNumber: page number must be greater than or equal to 0");

    Optional<PageRequestDto> pageRequestDtoInvalid =
        Optional.of(new PageRequestDto(1, 10000, null, Set.of("invalid")));
    jobExecutionFilterDtoInvalidPage.setPageRequestDto(pageRequestDtoInvalid);

    assertThatThrownBy(() -> testSubject.getJobsExecutions(jobExecutionFilterDtoInvalidPage))
        .isInstanceOf(ConstraintViolationException.class)
        .hasMessageContaining(
            "getJobsExecutions.jobExecutionFilterDto.url: url must be a valid URL")
        .hasMessageContaining(
            "getJobsExecutions.jobExecutionFilterDto.pageRequestDto.pageSize: page size max value allowed is 100")
        .hasMessageContaining(
            "getJobsExecutions.jobExecutionFilterDto.pageRequestDto.sortProperties.invalid: invalid field");

    verifyNoInteractions(jobExecutionRepositoryMock);
  }

  @Test
  void getJobsExecutions_invalidDateRange_throwSEInvalidDateRange() {

    JobExecutionFilterDto jobExecutionFilterDto =
        new JobExecutionFilterDto(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(Instant.parse("2024-09-07T11:44:10Z")),
            Optional.of(Instant.parse("2024-09-07T10:44:10Z")),
            Optional.empty());

    String errorMessage = "Invalid date range. From must be before to and to must be after from";

    assertThatThrownBy(() -> testSubject.getJobsExecutions(jobExecutionFilterDto))
        .hasMessage(errorMessage)
        .isInstanceOfSatisfying(
            StandardException.class,
            e -> {
              assertThat(e.isTech()).isFalse();
              assertThat(e.getErrorType()).isEqualTo(StandardException.ErrorType.FUNC);
              assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
              assertThat(e.getProblemDetail().getStatus()).isEqualTo(400);
              assertThat(e.getProblemDetail().getTitle()).isEqualTo("Invalid date range");
              assertThat(e.getProblemDetail().getCode()).isEqualTo("invalid_date_range");
              assertThat(e.getProblemDetail().getDetail()).isEqualTo(errorMessage);
            });

    verifyNoInteractions(jobExecutionRepositoryMock);
  }

  @ParameterizedTest
  @MethodSource("getJobsExecutionsInput")
  void getJobsExecutions_oneFound_succeed(
      final JobExecutionFilterDto jobExecutionFilterDto, final PageRequest pageRequest) {

    JobExecutionEntity jobExecutionEntity =
        JobExecutionEntity.builder()
            .jobName("google")
            .url("https://www.google.com")
            .errorMessage("404 not found")
            .status(JobExecutionStatusEntity.FAILED)
            .responseTime(80L)
            .id(1L)
            .externalId(UUID.randomUUID())
            .createdBy("test-user")
            .createdDate(Instant.now())
            .lastModifiedBy("test-user")
            .lastModifiedDate(Instant.now())
            .traceId("123456789")
            .build();

    when(jobExecutionRepositoryMock.findByCriteria(
            new JobExecutionCriteriaParams().setPageable(pageRequest)))
        .thenReturn(new PageImpl<>(List.of(jobExecutionEntity), pageRequest, 1));

    JobExecutionDtoList result = testSubject.getJobsExecutions(jobExecutionFilterDto);

    assertThat(result.getTotalPages()).isOne();
    assertThat(result.getTotalElements()).isEqualTo(1L);
    assertThat(result.getJobExecutionList()).hasSize(1);

    assertThat(result.getJobExecutionList().getFirst())
        .usingRecursiveComparison()
        .ignoringExpectedNullFields()
        .ignoringFields("externalId")
        .isEqualTo(jobExecutionEntity);

    verify(jobExecutionRepositoryMock)
        .findByCriteria(new JobExecutionCriteriaParams().setPageable(pageRequest));
    verifyNoMoreInteractions(jobExecutionRepositoryMock);
  }

  private static Stream<Arguments> getJobsExecutionsInput() {

    Sort defaultSort =
        Sort.sort(JobExecutionEntity.class).by(JobExecutionEntity::getCreatedDate).descending();

    return Stream.of(
        // all fields null in pagination (apply default pagination)
        Arguments.of(
            new JobExecutionFilterDto(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(new PageRequestDto(null, null, null, null))),
            PageRequest.of(0, 100, defaultSort)),
        // only sort direction null (apply default sort)
        Arguments.of(
            new JobExecutionFilterDto(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(new PageRequestDto(0, 10, null, Set.of("createdDate")))),
            PageRequest.of(0, 10, defaultSort)),
        // apply sort direction and sort property
        Arguments.of(
            new JobExecutionFilterDto(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(
                    new PageRequestDto(
                        0, 10, PageRequestDto.SortDirection.DESC, Set.of("createdDate")))),
            PageRequest.of(0, 10, defaultSort)),
        // only sort properties null (apply default sort)
        Arguments.of(
            new JobExecutionFilterDto(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(new PageRequestDto(0, 10, PageRequestDto.SortDirection.ASC, null))),
            PageRequest.of(0, 10, defaultSort)),
        // no page (apply default sort)
        Arguments.of(
            new JobExecutionFilterDto(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()),
            PageRequest.of(0, 100, defaultSort)));
  }
}
