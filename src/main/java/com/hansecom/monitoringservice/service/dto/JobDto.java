package com.hansecom.monitoringservice.service.dto;

import com.hansecom.monitoringservice.service.validator.ValidCronExpression;
import com.hansecom.monitoringservice.service.validator.ValidTimezone;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

/** Data transfer object for Job */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class JobDto {

  @NotBlank(message = "name {jakarta.validation.constraints.NotBlank.message}")
  private String name;

  @NotBlank(message = "description {jakarta.validation.constraints.NotBlank.message}")
  private String description;

  @NotBlank(message = "url {jakarta.validation.constraints.NotBlank.message}")
  @URL(message = "url {org.hibernate.validator.constraints.URL.message}")
  private String url;

  @ValidCronExpression(message = "cronExpression not a valid quartz cron expression")
  private String cronExpression;

  @ValidTimezone(message = "timezone invalid timezone ID")
  private String timezone;
}
