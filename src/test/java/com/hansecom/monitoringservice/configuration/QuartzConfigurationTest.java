package com.hansecom.monitoringservice.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Scheduler;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

@ExtendWith(MockitoExtension.class)
class QuartzConfigurationTest {

  @InjectMocks private QuartzConfiguration testSubject;

  @Mock private SchedulerFactoryBean factoryMock;

  @Mock private Scheduler schedulerMock;

  @SneakyThrows
  @Test
  void scheduler() {

    when(factoryMock.getScheduler()).thenReturn(schedulerMock);

    Scheduler result = testSubject.scheduler(factoryMock);

    assertThat(result).isSameAs(schedulerMock);

    verify(factoryMock).getScheduler();
    verifyNoMoreInteractions(factoryMock);

    verify(schedulerMock).start();
    verifyNoMoreInteractions(schedulerMock);
  }
}
