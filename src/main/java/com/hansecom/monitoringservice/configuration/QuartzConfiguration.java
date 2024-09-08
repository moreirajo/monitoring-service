package com.hansecom.monitoringservice.configuration;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

/** Configuration class for Quartz. */
@Configuration
public class QuartzConfiguration {

  /**
   * Provides a {@link Scheduler} bean.
   *
   * @param factory the {@link SchedulerFactoryBean} to get the {@link Scheduler}.
   * @return the {@link Scheduler} bean
   * @throws SchedulerException if so error occurs
   */
  @Bean()
  public Scheduler scheduler(SchedulerFactoryBean factory) throws SchedulerException {
    Scheduler scheduler = factory.getScheduler();
    scheduler.start();
    return scheduler;
  }
}
