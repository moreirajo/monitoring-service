package com.hansecom.monitoringservice.persistence.test.util.security;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.springframework.security.test.context.support.WithSecurityContext;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockCustomAuthTokenSecurityContextFactory.class)
public @interface WithMockCustomAuthToken {

  String principal() default "test-user";

  boolean isAuthenticated() default true;
}
