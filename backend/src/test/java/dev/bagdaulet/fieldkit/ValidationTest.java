package dev.bagdaulet.fieldkit;

import static org.testng.Assert.*;

import java.util.*;
import org.testng.annotations.*;

public class ValidationTest extends ApiTestSupport {
  @DataProvider
  public Object[][] gearBodies() {
    return new Object[][] {
      {"{\"name\":\"\",\"category\":\"CAMERAS\",\"totalQuantity\":1}", 422},
      {"{\"name\":123,\"category\":\"CAMERAS\",\"totalQuantity\":1}", 400},
      {"{\"name\":\"Kit\",\"category\":\"OTHER\",\"totalQuantity\":1}", 422},
      {"{\"name\":\"Kit\",\"category\":\"CAMERAS\",\"totalQuantity\":0}", 422},
      {"{\"name\":\"Kit\",\"category\":\"CAMERAS\",\"totalQuantity\":1001}", 422},
      {"{\"name\":\"Kit\",\"category\":\"CAMERAS\",\"totalQuantity\":1.5}", 400},
      {"{\"name\":\"Kit\",\"category\":\"CAMERAS\",\"totalQuantity\":\"1\"}", 400},
      {
        "{\"name\":\"Kit\",\"category\":\"CAMERAS\",\"totalQuantity\":1,\"workspaceId\":\"other\"}",
        400
      },
      {"[]", 400},
      {"null", 400},
      {"{bad", 400}
    };
  }

  @Test(dataProvider = "gearBodies")
  public void invalidEquipmentHasNoSideEffects(String body, int status) throws Exception {
    Client c = client();
    assertEquals(send(c, "POST", "/equipment", body, true).statusCode(), status);
    assertEquals(ok(c, "GET", "/equipment", null, 200).path("totalItems").asInt(), 0);
  }

  @DataProvider
  public Object[][] invalidQueries() {
    return new Object[][] {
      {"page=0"},
      {"pageSize=51"},
      {"status=FAKE"},
      {"category=FAKE"},
      {"owner=somebody"},
      {"page=1&page=2"}
    };
  }

  @Test(dataProvider = "invalidQueries")
  public void strictQueries(String query) throws Exception {
    Client c = client();
    ok(c, "GET", "/equipment?" + query, null, 422);
  }

  @Test
  public void searchPaginationAndEscapedWildcards() throws Exception {
    Client c = client();
    for (int i = 0; i < 3; i++) gear(c, 1);
    assertEquals(
        ok(c, "GET", "/equipment?search=CAMERA&pageSize=2", null, 200).path("items").size(), 2);
    assertEquals(ok(c, "GET", "/equipment?search=%25", null, 200).path("totalItems").asInt(), 0);
    assertEquals(ok(c, "GET", "/equipment?page=2&pageSize=2", null, 200).path("items").size(), 1);
  }

  @DataProvider
  public Object[][] badDates() {
    return new Object[][] {{"1999-01-01"}, {"2099-12-31"}, {"2026-02-30"}, {"tomorrow"}};
  }

  @Test(dataProvider = "badDates")
  public void invalidLoanDatesDoNotChangeStock(String date) throws Exception {
    Client c = client();
    String gear = gear(c, 1), person = person(c);
    var body = new HashMap<>(loan(gear, person, 1));
    body.put("dueDate", date);
    ok(c, "POST", "/loans", body, 422);
    assertEquals(
        ok(c, "GET", "/equipment/" + gear, null, 200).path("availableQuantity").asInt(), 1);
  }

  @Test
  public void oversizedRequestIsRejectedBeforeParsing() throws Exception {
    Client c = client();
    assertEquals(
        send(c, "POST", "/equipment", "{\"name\":\"" + "x".repeat(33000) + "\"}", true)
            .statusCode(),
        413);
  }

  @Test
  public void readableUnicodeAndLiteralMarkupArePreserved() throws Exception {
    Client c = client();
    var data =
        ok(
            c,
            "POST",
            "/borrowers",
            Map.of("name", "Дана <script>literal</script>", "team", "Creative studio"),
            201);
    assertEquals(data.path("name").asString(), "Дана <script>literal</script>");
    assertEquals(
        send(c, "POST", "/borrowers", "{\"name\":\"Bad\\u0001name\"}", true).statusCode(), 422);
  }

  @Test
  public void browserHeadersAndHealth() throws Exception {
    Client c = client();
    var response = send(c, "GET", "/health", null, false);
    assertEquals(response.statusCode(), 200);
    assertTrue(
        response
            .headers()
            .firstValue("content-security-policy")
            .orElse("")
            .contains("script-src 'self'"));
    assertEquals(response.headers().firstValue("x-frame-options").orElse(""), "DENY");
    assertEquals(response.headers().firstValue("cache-control").orElse(""), "no-store");
  }
}
