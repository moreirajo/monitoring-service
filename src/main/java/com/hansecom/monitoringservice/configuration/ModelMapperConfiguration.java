package com.hansecom.monitoringservice.configuration;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration class for mapping java objects. */
@Configuration
public class ModelMapperConfiguration {

  /**
   * ModelMapper bean.
   *
   * @return a {@link ModelMapper} instance
   */
  @Bean
  public ModelMapper modelMapper() {
    return new ModelMapper();
  }
}
