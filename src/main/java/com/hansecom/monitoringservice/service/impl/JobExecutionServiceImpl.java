package com.hansecom.monitoringservice.service.impl;

import com.hansecom.monitoringservice.exception.StandardException;
import com.hansecom.monitoringservice.persistence.model.JobExecutionCriteriaParams;
import com.hansecom.monitoringservice.persistence.model.JobExecutionEntity;
import com.hansecom.monitoringservice.persistence.repository.JobExecutionRepository;
import com.hansecom.monitoringservice.service.JobExecutionService;
import com.hansecom.monitoringservice.service.dto.JobExecutionDto;
import com.hansecom.monitoringservice.service.dto.JobExecutionDtoList;
import com.hansecom.monitoringservice.service.dto.JobExecutionFilterDto;
import com.hansecom.monitoringservice.service.dto.PageRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/** Implementation service of {@link JobExecutionService}. */
@Service
@RequiredArgsConstructor
public class JobExecutionServiceImpl implements JobExecutionService {

  private final JobExecutionRepository jobExecutionRepository;

  private final ModelMapper mapper;

  @Value("${custom-properties.database.pagination.max-page-size:100}")
  private int maxPageSize;

  @Override
  public JobExecutionDtoList getJobsExecutions(
      @NotNull @Valid JobExecutionFilterDto jobExecutionFilterDto) {

    Optional<Instant> from = jobExecutionFilterDto.getFrom();
    Optional<Instant> to = jobExecutionFilterDto.getTo();
    if (from.isPresent()
        && to.isPresent()
        && (from.get().isAfter(to.get()) || to.get().isBefore(from.get()))) {
      throw StandardException.invalidDateRange();
    }

    JobExecutionCriteriaParams jobExecutionCriteriaParams =
        mapper.map(jobExecutionFilterDto, JobExecutionCriteriaParams.class);
    jobExecutionCriteriaParams.setPageable(pageRequest(jobExecutionFilterDto.getPageRequestDto()));

    Page<JobExecutionEntity> result =
        jobExecutionRepository.findByCriteria(jobExecutionCriteriaParams);

    List<JobExecutionDto> jobExecutionDtoList =
        result
            .map(jobExecutionEntity -> mapper.map(jobExecutionEntity, JobExecutionDto.class))
            .toList();

    return JobExecutionDtoList.builder()
        .jobExecutionList(jobExecutionDtoList)
        .totalPages(result.getTotalPages())
        .totalElements(result.getTotalElements())
        .build();
  }

  /**
   * Converts the input {@link PageRequestDto} into a {@link PageRequest}. Applies default values
   * for pagination if not specified in the input.
   *
   * <p>Default values:
   *
   * <ul>
   *   <li>pageNumber: 0
   *   <li>pageSize: 100 (specified as an application property, so it may be different)
   *   <li>sortDirection: DESC
   *   <li>sortProperties: createdDate
   * </ul>
   *
   * @param pageRequestDto input dto
   * @return a {@link PageRequest} based on input and default values
   */
  private PageRequest pageRequest(Optional<PageRequestDto> pageRequestDto) {

    int pageNumber = 0;
    int pageSize = maxPageSize;
    Sort sort =
        Sort.sort(JobExecutionEntity.class).by(JobExecutionEntity::getCreatedDate).descending();

    if (pageRequestDto.isPresent()) {
      if (pageRequestDto.get().pageNumber() != null) {
        pageNumber = pageRequestDto.get().pageNumber();
      }

      if (pageRequestDto.get().pageSize() != null) {
        pageSize = pageRequestDto.get().pageSize();
      }

      if (pageRequestDto.get().sortDirection() != null
          && CollectionUtils.isNotEmpty(pageRequestDto.get().sortProperties())) {
        sort =
            Sort.by(
                Sort.Direction.fromString(pageRequestDto.get().sortDirection().name()),
                pageRequestDto.get().sortProperties().toArray(String[]::new));
      }
    }

    return PageRequest.of(pageNumber, pageSize, sort);
  }
}
