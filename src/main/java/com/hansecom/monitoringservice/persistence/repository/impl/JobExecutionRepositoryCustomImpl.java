package com.hansecom.monitoringservice.persistence.repository.impl;

import com.hansecom.monitoringservice.persistence.model.AbstractModelEntity_;
import com.hansecom.monitoringservice.persistence.model.JobExecutionCriteriaParams;
import com.hansecom.monitoringservice.persistence.model.JobExecutionEntity;
import com.hansecom.monitoringservice.persistence.model.JobExecutionEntity_;
import com.hansecom.monitoringservice.persistence.repository.JobExecutionRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.stereotype.Repository;

/** Implementation for Job Execution custom queries. */
@Repository
public class JobExecutionRepositoryCustomImpl implements JobExecutionRepositoryCustom {

  @PersistenceContext private EntityManager entityManager;

  @Override
  public Page<JobExecutionEntity> findByCriteria(
      @NotNull @Valid JobExecutionCriteriaParams jobExecutionCriteriaParams) {

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();

    CriteriaQuery<JobExecutionEntity> criteria = builder.createQuery(JobExecutionEntity.class);
    Root<JobExecutionEntity> csrRoot = criteria.from(JobExecutionEntity.class);

    List<Predicate> predicates = getPredicates(jobExecutionCriteriaParams, builder, csrRoot);

    criteria.where(builder.and(predicates.toArray(new Predicate[0])));

    Pageable pageable = jobExecutionCriteriaParams.getPageable();
    criteria.orderBy(QueryUtils.toOrders(pageable.getSort(), csrRoot, builder));

    TypedQuery<JobExecutionEntity> query = entityManager.createQuery(criteria);

    if (pageable.isPaged()) {
      query.setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize());
    }

    List<JobExecutionEntity> result = query.getResultList();

    Long count = countRecords(jobExecutionCriteriaParams);

    return new PageImpl<>(result, pageable, count);
  }

  /**
   * Method that counts the records matching the params.
   *
   * @param jobExecutionCriteriaParams parameters to filter by
   * @return number of records matching the params
   */
  private Long countRecords(JobExecutionCriteriaParams jobExecutionCriteriaParams) {

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
    Root<JobExecutionEntity> csrRootCount = countQuery.from(JobExecutionEntity.class);

    List<Predicate> predicates = getPredicates(jobExecutionCriteriaParams, builder, csrRootCount);
    countQuery
        .select(builder.count(csrRootCount))
        .where(builder.and(predicates.toArray(new Predicate[0])));

    return entityManager.createQuery(countQuery).getSingleResult();
  }

  /**
   * Method that returns a list of predicates based on the parameters.
   *
   * @param jobExecutionCriteriaParams parameters to filter by
   * @param builder the criteria builder
   * @param csrRoot the root object
   * @return list of predicates
   */
  private static List<Predicate> getPredicates(
      JobExecutionCriteriaParams jobExecutionCriteriaParams,
      CriteriaBuilder builder,
      Root<JobExecutionEntity> csrRoot) {

    List<Predicate> predicates = new ArrayList<>();

    jobExecutionCriteriaParams
        .getJobName()
        .ifPresent(
            jobName ->
                predicates.add(builder.equal(csrRoot.get(JobExecutionEntity_.JOB_NAME), jobName)));

    jobExecutionCriteriaParams
        .getUrl()
        .ifPresent(url -> predicates.add(builder.equal(csrRoot.get(JobExecutionEntity_.URL), url)));

    jobExecutionCriteriaParams
        .getStatus()
        .ifPresent(
            status ->
                predicates.add(builder.equal(csrRoot.get(JobExecutionEntity_.STATUS), status)));

    jobExecutionCriteriaParams
        .getFrom()
        .ifPresent(
            from ->
                predicates.add(
                    builder.greaterThanOrEqualTo(
                        csrRoot.get(AbstractModelEntity_.CREATED_DATE), from)));

    jobExecutionCriteriaParams
        .getTo()
        .ifPresent(
            to ->
                predicates.add(
                    builder.lessThanOrEqualTo(csrRoot.get(AbstractModelEntity_.CREATED_DATE), to)));

    return predicates;
  }
}
