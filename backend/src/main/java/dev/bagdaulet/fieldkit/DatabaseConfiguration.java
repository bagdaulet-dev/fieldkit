package dev.bagdaulet.fieldkit;

import java.time.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class DatabaseConfiguration {
  @Bean
  Clock clock(@Value("${fieldkit.time-zone}") String zone) {
    return Clock.system(ZoneId.of(zone));
  }

  @Bean
  TransactionTemplate transactions(
      org.springframework.transaction.PlatformTransactionManager manager) {
    return new TransactionTemplate(manager);
  }
}
