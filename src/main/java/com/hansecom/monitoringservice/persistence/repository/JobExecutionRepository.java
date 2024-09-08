package com.hansecom.monitoringservice.persistence.repository;

import com.hansecom.monitoringservice.persistence.model.JobExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Interface for {@link JobExecutionEntity} database operations.
 *
 * @see JpaRepository
 */
public interface JobExecutionRepository
    extends JpaRepository<JobExecutionEntity, Long>, JobExecutionRepositoryCustom {}
