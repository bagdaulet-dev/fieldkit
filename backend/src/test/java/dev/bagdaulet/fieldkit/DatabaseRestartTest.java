package dev.bagdaulet.fieldkit;

import static org.testng.Assert.*;

import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testng.annotations.Test;
import tools.jackson.databind.json.JsonMapper;

public class DatabaseRestartTest {
  @Test
  public void fileDatabaseMigrationsAndSessionSurviveRestart() throws Exception {
    Path folder = Files.createTempDirectory("fieldkit-restart-");
    String url =
        "jdbc:h2:file:" + folder.resolve("workspace") + ";MODE=MySQL;DB_CLOSE_ON_EXIT=FALSE";
    JsonMapper json = JsonMapper.builder().build();
    try (HttpClient client =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build()) {
      try (ConfigurableApplicationContext first = start(url)) {
        String base = base(first);
        var session =
            client.send(
                HttpRequest.newBuilder(URI.create(base + "/session")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        String token = json.readTree(session.body()).path("csrfToken").asString();
        var created =
            client.send(
                HttpRequest.newBuilder(URI.create(base + "/equipment"))
                    .header("Content-Type", "application/json")
                    .header("X-CSRF-TOKEN", token)
                    .POST(
                        HttpRequest.BodyPublishers.ofString(
                            "{\"name\":\"Persistent kit\",\"category\":\"CAMERAS\",\"totalQuantity\":2}"))
                    .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(created.statusCode(), 201, created.body());
      }
      try (ConfigurableApplicationContext second = start(url)) {
        var records =
            client.send(
                HttpRequest.newBuilder(URI.create(base(second) + "/equipment")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(records.statusCode(), 200, records.body());
        var result = json.readTree(records.body());
        assertEquals(result.path("totalItems").asInt(), 1);
        assertEquals(result.path("items").get(0).path("name").asString(), "Persistent kit");
      }
    } finally {
      Files.deleteIfExists(folder.resolve("workspace.mv.db"));
      Files.deleteIfExists(folder.resolve("workspace.trace.db"));
      Files.delete(folder);
    }
  }

  private ConfigurableApplicationContext start(String url) {
    return new SpringApplicationBuilder(FieldkitApplication.class)
        .profiles("preview")
        .run(
            "--server.port=0",
            "--spring.datasource.url=" + url,
            "--debug=false",
            "--logging.level.root=ERROR");
  }

  private String base(ConfigurableApplicationContext app) {
    return "http://127.0.0.1:" + app.getEnvironment().getProperty("local.server.port") + "/api";
  }
}
