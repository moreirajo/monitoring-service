package com.hansecom.monitoringservice.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.hansecom.monitoringservice.persistence.model.JobExecutionCriteriaParams;
import com.hansecom.monitoringservice.persistence.model.JobExecutionEntity;
import com.hansecom.monitoringservice.persistence.model.JobExecutionStatusEntity;
import com.hansecom.monitoringservice.persistence.test.util.DatabaseTestSetup;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.jdbc.Sql;

@DatabaseTestSetup
class JobExecutionRepositoryTest {

  @Autowired private JobExecutionRepository testSubject;

  @Test
  void save_oneEntity_succeed() {

    JobExecutionEntity entity =
        JobExecutionEntity.builder()
            .jobName("google")
            .url("https://www.google.com")
            .errorMessage("404 not found")
            .status(JobExecutionStatusEntity.FAILED)
            .responseTime(80L)
            .build();

    JobExecutionEntity saved = testSubject.save(entity);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getExternalId()).isNotNull();
    assertThat(saved.getCreatedDate()).isCloseTo(Instant.now(), within(30, ChronoUnit.SECONDS));
    assertThat(saved.getLastModifiedDate())
        .isCloseTo(Instant.now(), within(30, ChronoUnit.SECONDS));
    assertThat(saved.getCreatedBy()).isEqualTo("test-user");
    assertThat(saved.getLastModifiedBy()).isEqualTo("test-user");
    assertThat(saved.getTraceId()).isEqualTo("00000000000000000000000000000000");

    assertThat(saved.getJobName()).isEqualTo("google");
    assertThat(saved.getUrl()).isEqualTo("https://www.google.com");
    assertThat(saved.getErrorMessage()).isEqualTo("404 not found");
    assertThat(saved.getStatus()).isEqualTo(JobExecutionStatusEntity.FAILED);
    assertThat(saved.getResponseTime()).isEqualTo(80L);
  }

  @Test
  void save_emptyEntity_throwDataIntegrityViolationException() {

    JobExecutionEntity entity = JobExecutionEntity.builder().build();

    assertThatThrownBy(() -> testSubject.save(entity))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageMatching(
            "could not execute statement \\[ERROR: null value in column \".*\" of relation \".*\" violates not-null constraint\n.*");
  }

  @Test
  void findByCriteria_invalidInput_throwConstraintViolationException() {

    assertThatThrownBy(() -> testSubject.findByCriteria(null))
        .isInstanceOf(ConstraintViolationException.class)
        .hasMessage("findByCriteria.jobExecutionCriteriaParams: must not be null");

    JobExecutionCriteriaParams jobExecutionCriteriaParams =
        new JobExecutionCriteriaParams(null, null, null, null, null, null);

    assertThatThrownBy(() -> testSubject.findByCriteria(jobExecutionCriteriaParams))
        .isInstanceOf(ConstraintViolationException.class)
        .hasMessageContaining("findByCriteria.jobExecutionCriteriaParams.jobName: must not be null")
        .hasMessageContaining("findByCriteria.jobExecutionCriteriaParams.url: must not be null")
        .hasMessageContaining("findByCriteria.jobExecutionCriteriaParams.status: must not be null")
        .hasMessageContaining("findByCriteria.jobExecutionCriteriaParams.from: must not be null")
        .hasMessageContaining("findByCriteria.jobExecutionCriteriaParams.to: must not be null")
        .hasMessageContaining(
            "findByCriteria.jobExecutionCriteriaParams.pageable: must not be null");

    assertDoesNotThrow(() -> testSubject.findByCriteria(new JobExecutionCriteriaParams()));
  }

  @ParameterizedTest
  @MethodSource("findByCriteriaInputs")
  @Sql("/db/insert_test_job_execution.sql")
  void findByCriteria_severalInputs_succeed(
      final JobExecutionCriteriaParams jobExecutionCriteriaParams,
      final Long totalElements,
      final Integer totalPages,
      final Integer totalElementsInPage,
      final Long expectedFirstId,
      final Long expectedLastId) {

    Page<JobExecutionEntity> result = testSubject.findByCriteria(jobExecutionCriteriaParams);

    assertThat(result.getTotalElements()).isEqualTo(totalElements);
    assertThat(result.getTotalPages()).isEqualTo(totalPages);
    assertThat(result.get()).hasSize(totalElementsInPage);

    if (totalElementsInPage > 0) {
      assertThat(result.toList().getFirst().getId()).isEqualTo(expectedFirstId);
      assertThat(result.toList().getLast().getId()).isEqualTo(expectedLastId);
    }
  }

  private static Stream<Arguments> findByCriteriaInputs() {

    // jobExecutionCriteriaParams, totalElements, totalPages, totalElementsInPage, expectedFirstId,
    // expectedLastId

    return Stream.of(
        // #1 Filter by JobName only
        Arguments.of(
            new JobExecutionCriteriaParams().setJobName(Optional.of("google")), 2L, 1, 2, 1L, 2L),
        // #2 Filter by JobName and url
        Arguments.of(
            new JobExecutionCriteriaParams()
                .setJobName(Optional.of("google"))
                .setUrl(Optional.of("https://www.google.com")),
            2L,
            1,
            2,
            1L,
            2L),
        // #3 Filter by status
        Arguments.of(
            new JobExecutionCriteriaParams()
                .setStatus(Optional.of(JobExecutionStatusEntity.FAILED)),
            1L,
            1,
            1,
            3L,
            3L),
        // #4 Filter by JobName and url no results
        Arguments.of(
            new JobExecutionCriteriaParams()
                .setJobName(Optional.of("google"))
                .setUrl(Optional.of("https://www.google2.com")),
            0L,
            1,
            0,
            null,
            null),
        // #5 Filter by from
        Arguments.of(
            new JobExecutionCriteriaParams()
                .setFrom(Optional.of(Instant.parse("2023-01-01T12:01:00.000000Z"))),
            2L,
            1,
            2,
            2L,
            3L),
        // #6 Filter by to
        Arguments.of(
            new JobExecutionCriteriaParams()
                .setTo(Optional.of(Instant.parse("2023-01-01T12:01:00.000000Z"))),
            2L,
            1,
            2,
            1L,
            2L),
        // #7 Filter by from and to
        Arguments.of(
            new JobExecutionCriteriaParams()
                .setFrom(Optional.of(Instant.parse("2023-01-01T12:01:00.000000Z")))
                .setTo(Optional.of(Instant.parse("2023-01-01T12:01:00.000000Z"))),
            1L,
            1,
            1,
            2L,
            2L),
        // #8 No filter
        Arguments.of(new JobExecutionCriteriaParams(), 3L, 1, 3, 1L, 3L),
        // #9 Last page sort asc
        Arguments.of(
            new JobExecutionCriteriaParams()
                .setPageable(
                    PageRequest.of(
                        1,
                        2,
                        Sort.sort(JobExecutionEntity.class)
                            .by(JobExecutionEntity::getCreatedDate)
                            .ascending())),
            3L,
            2,
            1,
            3L,
            3L),
        // #10 Last page sort desc
        Arguments.of(
            new JobExecutionCriteriaParams()
                .setPageable(
                    PageRequest.of(
                        1,
                        2,
                        Sort.sort(JobExecutionEntity.class)
                            .by(JobExecutionEntity::getCreatedDate)
                            .descending())),
            3L,
            2,
            1,
            1L,
            1L));
  }
}
