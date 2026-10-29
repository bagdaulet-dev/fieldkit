package dev.bagdaulet.fieldkit;

import java.util.*;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

/** Reject unsafe deployment settings before creating database connections or running migrations. */
@Configuration
public class DeploymentPolicy {
  @Bean
  static BeanFactoryPostProcessor validateDeployment(Environment env) {
    verify(env);
    return factory -> {};
  }

  static void verify(Environment env) {
    if (env.matchesProfiles("mysql")) {
      String user = env.getProperty("spring.datasource.username", "").strip();
      String password = env.getProperty("spring.datasource.password", "");
      if (user.isBlank()
          || user.equalsIgnoreCase("root")
          || password.isBlank()
          || password.toLowerCase(Locale.ROOT).startsWith("change-me"))
        throw new IllegalStateException(
            "Configure a dedicated MySQL user and a non-placeholder password.");
    }
    if (env.matchesProfiles("production")) {
      if (!env.matchesProfiles("mysql")
          || !env.getProperty("fieldkit.secure-cookies", Boolean.class, false)
          || env.getProperty("fieldkit.demo-enabled", Boolean.class, true))
        throw new IllegalStateException(
            "Production requires MySQL, secure cookies and disabled demo seeding.");
      String url = env.getProperty("spring.datasource.url", "");
      String[] options = url.substring(url.indexOf('?') + 1).split("&");
      long tlsOptions =
          Arrays.stream(options)
              .filter(option -> option.regionMatches(true, 0, "sslMode=", 0, 8))
              .count();
      if (!url.startsWith("jdbc:mysql://")
          || tlsOptions != 1
          || Arrays.stream(options)
              .noneMatch(option -> option.equalsIgnoreCase("sslMode=VERIFY_IDENTITY")))
        throw new IllegalStateException(
            "Production requires MySQL TLS with sslMode=VERIFY_IDENTITY.");
    }
  }
}
