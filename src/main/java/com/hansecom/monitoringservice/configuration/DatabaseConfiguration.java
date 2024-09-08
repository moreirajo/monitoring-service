package com.hansecom.monitoringservice.configuration;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/** Provides database related configuration. */
@EnableJpaAuditing
@Configuration
public class DatabaseConfiguration {

  /**
   * Provides the user that made the request.
   *
   * <p>This information will be used by {@link CreatedBy} and {@link LastModifiedBy} to store the
   * user that made the request on the database along with the entity.
   *
   * @return the clientId from the access token
   */
  @Bean
  AuditorAware<String> auditorAware() {

    return () ->
        Optional.ofNullable(SecurityContextHolder.getContext())
            .map(SecurityContext::getAuthentication)
            .filter(Authentication::isAuthenticated)
            .map(Authentication::getPrincipal)
            .map(Object::toString)
            .or(() -> Optional.ofNullable(System.getProperty("user.name")));
  }
}
