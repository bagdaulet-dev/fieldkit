package dev.bagdaulet.fieldkit;

import static org.testng.Assert.*;

import java.net.*;
import java.net.http.*;
import java.util.*;
import org.testng.annotations.*;

public class SecurityTest extends ApiTestSupport {
  @Test
  public void csrfTokenFromAnotherWorkspaceCannotAuthorizeWrites() throws Exception {
    Client a = client(), b = client();
    var request =
        HttpRequest.newBuilder(URI.create(base + "/borrowers"))
            .header("Content-Type", "application/json")
            .header("X-CSRF-TOKEN", a.token())
            .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"Unwanted write\"}"))
            .build();
    assertEquals(b.http().send(request, HttpResponse.BodyHandlers.ofString()).statusCode(), 403);
    assertEquals(ok(b, "GET", "/borrowers", null, 200).path("totalItems").asInt(), 0);
  }

  @Test
  public void sessionIdentifiersInQueryCannotSelectAnotherWorkspace() throws Exception {
    Client owner = client(), stranger = client();
    gear(owner, 1);
    var cookies =
        ((CookieManager) owner.http().cookieHandler().orElseThrow()).getCookieStore().getCookies();
    String cookie =
        cookies.stream()
            .filter(c -> c.getName().equals("FIELDKIT_SESSION"))
            .findFirst()
            .orElseThrow()
            .getValue();
    assertEquals(
        ok(
                stranger,
                "GET",
                "/dashboard?FIELDKIT_SESSION="
                    + URLEncoder.encode(cookie, java.nio.charset.StandardCharsets.UTF_8),
                null,
                200)
            .path("equipmentTypes")
            .asInt(),
        0);
  }

  @DataProvider
  public Object[][] privatePaths() {
    return new Object[][] {
      {"/.env"},
      {"/.git/config"},
      {"/.local/preview.mv.db"},
      {"/application-mysql.properties"},
      {"/backend/src/main/resources/application.properties"},
      {"/h2-console"}
    };
  }

  @Test(dataProvider = "privatePaths")
  public void privateFilesAndDatabaseConsoleAreNotServed(String path) throws Exception {
    Client c = client();
    var response =
        c.http()
            .send(
                HttpRequest.newBuilder(URI.create(base.replace("/api", "") + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    assertEquals(response.statusCode(), 404);
    assertEquals(response.headers().firstValue("x-content-type-options").orElse(""), "nosniff");
    assertTrue(
        response
            .headers()
            .firstValue("content-security-policy")
            .orElse("")
            .contains("frame-ancestors 'none'"));
    assertFalse(response.body().contains("spring.datasource"));
    assertFalse(response.body().contains("Exception"));
  }

  @Test
  public void crossOriginResponsesCannotBeReadAndPrivateResponsesAreNotCached() throws Exception {
    Client c = client();
    for (String path :
        List.of("/session", "/equipment", "/borrowers", "/loans", "/dashboard", "/export")) {
      var response =
          c.http()
              .send(
                  HttpRequest.newBuilder(URI.create(base + path))
                      .header("Origin", "https://untrusted.example")
                      .GET()
                      .build(),
                  HttpResponse.BodyHandlers.ofString());
      assertEquals(response.statusCode(), 200);
      assertTrue(response.headers().firstValue("access-control-allow-origin").isEmpty());
      assertTrue(response.headers().firstValue("cache-control").orElse("").contains("no-store"));
    }
  }
}
