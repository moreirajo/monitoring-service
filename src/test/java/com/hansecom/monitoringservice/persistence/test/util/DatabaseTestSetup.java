package com.hansecom.monitoringservice.persistence.test.util;

import com.hansecom.monitoringservice.configuration.DatabaseConfiguration;
import com.hansecom.monitoringservice.persistence.test.util.security.WithMockCustomAuthToken;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ActiveProfiles("test-db")
@DataJpaTest
@Import({DatabaseConfiguration.class, ValidationAutoConfiguration.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@WithMockCustomAuthToken
@Sql(scripts = "/db/clean_db.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public @interface DatabaseTestSetup {}
