package com.hansecom.monitoringservice.persistence.model;

import io.opentelemetry.api.trace.Span;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Common model for all database entities.
 *
 * <p>Focus on auditable columns.
 */
@SuperBuilder(toBuilder = true)
@Setter
@Getter
@NoArgsConstructor
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AbstractModelEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private UUID externalId;

  @CreatedDate private Instant createdDate;

  @LastModifiedDate private Instant lastModifiedDate;

  @CreatedBy private String createdBy;

  @LastModifiedBy private String lastModifiedBy;

  private String traceId;

  /** Fill some columns before persist. */
  @PrePersist
  public void prePersist() {
    if (externalId == null) {
      externalId = UUID.randomUUID();
    }
    traceId = Span.current().getSpanContext().getTraceId();
  }
}
