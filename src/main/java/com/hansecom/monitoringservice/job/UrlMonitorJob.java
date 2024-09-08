package com.hansecom.monitoringservice.job;

import com.hansecom.monitoringservice.persistence.model.JobExecutionEntity;
import com.hansecom.monitoringservice.persistence.model.JobExecutionStatusEntity;
import com.hansecom.monitoringservice.persistence.repository.JobExecutionRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/** Job responsible for monitoring an url. */
@Slf4j
@Component
@AllArgsConstructor
public class UrlMonitorJob implements Job {

  private static final String STOP_WATCH = "stopWatch";

  private final JobExecutionRepository jobExecutionRepository;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

    JobDataMap dataMap = jobExecutionContext.getJobDetail().getJobDataMap();
    String url = dataMap.getString("url");

    Mono.deferContextual(
            contextView ->
                WebClient.builder()
                    .baseUrl(url)
                    .build()
                    .get()
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnNext(
                        response -> {
                          long responseTime =
                              System.currentTimeMillis() - contextView.<Long>get(STOP_WATCH);

                          log.debug("Call to {} took {} ms", url, responseTime);

                          jobExecutionRepository.save(
                              JobExecutionEntity.builder()
                                  .jobName(jobExecutionContext.getJobDetail().getKey().getName())
                                  .url(url)
                                  .status(JobExecutionStatusEntity.SUCCEEDED)
                                  .responseTime(responseTime)
                                  .build());
                        })
                    .doOnError(
                        throwable -> {
                          long responseTime =
                              System.currentTimeMillis() - contextView.<Long>get(STOP_WATCH);

                          log.debug(
                              "Call to {} took {} ms with error {}",
                              url,
                              responseTime,
                              throwable.getMessage());

                          jobExecutionRepository.save(
                              JobExecutionEntity.builder()
                                  .jobName(jobExecutionContext.getJobDetail().getKey().getName())
                                  .url(url)
                                  .status(JobExecutionStatusEntity.FAILED)
                                  .responseTime(responseTime)
                                  .errorMessage(throwable.getMessage())
                                  .build());
                        }))
        .contextWrite(context -> context.put(STOP_WATCH, System.currentTimeMillis()))
        .block();
  }
}
