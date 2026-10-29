package dev.bagdaulet.fieldkit;

import static org.testng.Assert.*;

import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testng.annotations.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public abstract class ApiTestSupport {
  protected static ConfigurableApplicationContext app;
  protected static String base;
  protected static final JsonMapper json = JsonMapper.builder().build();
  protected static final List<HttpClient> clients = new CopyOnWriteArrayList<>();

  protected record Client(HttpClient http, String token) {}

  @BeforeSuite
  public void start() {
    String profile = System.getenv().getOrDefault("TEST_STORAGE", "preview");
    List<String> args =
        new ArrayList<>(
            List.of(
                "--server.port=0",
                "--logging.level.root=ERROR",
                "--debug=false",
                "--fieldkit.requests-per-minute=10000"));
    if (!profile.equals("mysql"))
      args.add(
          "--spring.datasource.url=jdbc:h2:mem:test-"
              + UUID.randomUUID()
              + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
    app =
        new SpringApplicationBuilder(FieldkitApplication.class)
            .profiles(profile)
            .run(args.toArray(String[]::new));
    base = "http://127.0.0.1:" + app.getEnvironment().getProperty("local.server.port") + "/api";
  }

  @AfterSuite(alwaysRun = true)
  public void stop() {
    clients.forEach(HttpClient::close);
    if (app != null) app.close();
  }

  protected Client client() throws Exception {
    HttpClient http =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    clients.add(http);
    var r =
        http.send(
            HttpRequest.newBuilder(URI.create(base + "/session")).GET().build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(r.statusCode(), 200, r.body());
    String cookie = r.headers().firstValue("set-cookie").orElse("");
    assertTrue(cookie.contains("HttpOnly"));
    assertTrue(cookie.contains("SameSite=Strict"));
    return new Client(http, json.readTree(r.body()).path("csrfToken").asString());
  }

  protected HttpResponse<String> send(
      Client client, String method, String path, String body, boolean csrf) throws Exception {
    var req =
        HttpRequest.newBuilder(URI.create(base + path))
            .timeout(Duration.ofSeconds(20))
            .header("Content-Type", "application/json");
    if (csrf) req.header("X-CSRF-TOKEN", client.token());
    return client
        .http()
        .send(
            req.method(
                    method,
                    body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString());
  }

  protected JsonNode ok(Client c, String method, String path, Object body, int expected)
      throws Exception {
    var r = send(c, method, path, body == null ? null : json.writeValueAsString(body), true);
    assertEquals(r.statusCode(), expected, r.body());
    return expected == 204 ? null : json.readTree(r.body());
  }

  protected String gear(Client c, int quantity) throws Exception {
    return ok(
            c,
            "POST",
            "/equipment",
            Map.of(
                "name",
                "Test camera",
                "model",
                "Kit One",
                "category",
                "CAMERAS",
                "totalQuantity",
                quantity),
            201)
        .path("id")
        .asString();
  }

  protected String person(Client c) throws Exception {
    return ok(c, "POST", "/borrowers", Map.of("name", "Nova Test", "team", "Media club"), 201)
        .path("id")
        .asString();
  }

  protected Map<String, Object> loan(String gear, String borrower, int quantity) {
    return Map.of(
        "equipmentId",
        gear,
        "borrowerId",
        borrower,
        "quantity",
        quantity,
        "dueDate",
        LocalDate.now(ZoneId.of("Asia/Almaty")).plusDays(3).toString(),
        "notes",
        "Fictional test loan");
  }
}
