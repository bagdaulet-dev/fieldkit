package dev.bagdaulet.fieldkit;

import static org.testng.Assert.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.*;
import org.testng.annotations.*;

public class DeploymentPolicyTest {
  private StandardEnvironment environment(Map<String, Object> overrides) {
    var env = new StandardEnvironment();
    env.setActiveProfiles("mysql", "production");
    var values =
        new HashMap<String, Object>(
            Map.of(
                "spring.datasource.username",
                "fieldkit",
                "spring.datasource.password",
                "test-policy-only",
                "spring.datasource.url",
                "jdbc:mysql://db/fieldkit?sslMode=VERIFY_IDENTITY",
                "fieldkit.secure-cookies",
                "true",
                "fieldkit.demo-enabled",
                "false"));
    values.putAll(overrides);
    env.getPropertySources().addFirst(new MapPropertySource("policy-test", values));
    return env;
  }

  @DataProvider
  public Object[][] unsafeSettings() {
    return new Object[][] {
      {"spring.datasource.username", "ROOT"},
      {"spring.datasource.username", " "},
      {"spring.datasource.password", ""},
      {"spring.datasource.password", "change-me-generated-by-setup-env"},
      {"fieldkit.secure-cookies", "false"},
      {"fieldkit.demo-enabled", "true"},
      {"spring.datasource.url", "jdbc:mysql://db/fieldkit?sslMode=REQUIRED"},
      {"spring.datasource.url", "jdbc:mysql://db/fieldkit?sslMode=DISABLED"},
      {
        "spring.datasource.url", "jdbc:mysql://db/fieldkit?sslMode=VERIFY_IDENTITY&sslMode=DISABLED"
      },
      {"spring.datasource.url", "jdbc:mysql://db/fieldkit"}
    };
  }

  @Test(dataProvider = "unsafeSettings")
  public void refusesUnsafeProductionConfiguration(String key, Object value) {
    expectThrows(
        IllegalStateException.class,
        () -> DeploymentPolicy.verify(environment(Map.of(key, value))));
  }

  @Test
  public void allowsVerifiedMysqlProductionAndLocalPreview() {
    DeploymentPolicy.verify(environment(Map.of()));
    var preview = new StandardEnvironment();
    preview.setActiveProfiles("preview");
    DeploymentPolicy.verify(preview);
  }

  @Test
  public void productionCannotUsePreviewStorage() {
    var env = environment(Map.of());
    env.setActiveProfiles("preview", "production");
    expectThrows(IllegalStateException.class, () -> DeploymentPolicy.verify(env));
  }

  @Test
  public void rejectsUnsafeSettingsBeforeCreatingApplicationBeans() {
    var created = new AtomicBoolean();
    try (var context = new AnnotationConfigApplicationContext()) {
      context.setEnvironment(environment(Map.of("spring.datasource.username", "root")));
      context.register(DeploymentPolicy.class);
      context.registerBean(
          "connectionSentinel",
          Object.class,
          () -> {
            created.set(true);
            return new Object();
          });
      expectThrows(org.springframework.beans.BeansException.class, context::refresh);
      assertFalse(
          created.get(), "No connection or migration bean may be created with unsafe settings.");
    }
  }
}
