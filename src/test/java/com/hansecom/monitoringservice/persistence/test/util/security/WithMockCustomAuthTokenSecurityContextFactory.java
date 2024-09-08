package com.hansecom.monitoringservice.persistence.test.util.security;

import java.util.ArrayList;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithMockCustomAuthTokenSecurityContextFactory
    implements WithSecurityContextFactory<WithMockCustomAuthToken> {

  @Override
  public SecurityContext createSecurityContext(final WithMockCustomAuthToken annotation) {
    final SecurityContext context = SecurityContextHolder.createEmptyContext();

    AbstractAuthenticationToken authentication =
        new AbstractAuthenticationToken(new ArrayList<>()) {
          @Override
          public Object getCredentials() {
            return null;
          }

          @Override
          public Object getPrincipal() {
            return annotation.principal();
          }

          @Override
          public boolean isAuthenticated() {
            return annotation.isAuthenticated();
          }
        };

    context.setAuthentication(authentication);
    return context;
  }
}
