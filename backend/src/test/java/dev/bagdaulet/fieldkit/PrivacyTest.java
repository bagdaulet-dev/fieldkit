package dev.bagdaulet.fieldkit;

import static org.testng.Assert.*;

import java.util.*;
import org.testng.annotations.*;

public class PrivacyTest extends ApiTestSupport {
  @Test
  public void independentClientsStartEmpty() throws Exception {
    Client a = client(), b = client();
    gear(a, 2);
    person(a);
    assertEquals(ok(b, "GET", "/dashboard", null, 200).path("equipmentTypes").asInt(), 0);
    assertEquals(ok(b, "GET", "/borrowers", null, 200).path("totalItems").asInt(), 0);
    assertEquals(ok(b, "GET", "/export", null, 200).path("equipment").size(), 0);
  }

  @DataProvider
  public Object[][] operations() {
    return new Object[][] {
      {"GET", "equipment"},
      {"PUT", "equipment"},
      {"DELETE", "equipment"},
      {"GET", "loans"},
      {"POST", "return"},
      {"PUT", "borrowers"},
      {"DELETE", "borrowers"}
    };
  }

  @Test(dataProvider = "operations")
  public void foreignRecordsCannotBeReadOrMutated(String method, String resource) throws Exception {
    Client a = client(), b = client();
    String gear = gear(a, 2), person = person(a);
    String id = ok(a, "POST", "/loans", loan(gear, person, 1), 201).path("id").asString();
    String path =
        resource.equals("return")
            ? "/loans/" + id + "/return"
            : "/"
                + resource
                + "/"
                + (resource.equals("equipment") ? gear : resource.equals("loans") ? id : person);
    Object body =
        method.equals("PUT")
            ? (resource.equals("equipment")
                ? Map.of("name", "Attempt", "category", "CAMERAS", "totalQuantity", 1)
                : Map.of("name", "Attempt"))
            : method.equals("POST") ? Map.of() : null;
    ok(b, method, path, body, 404);
    assertEquals(
        ok(a, "GET", "/equipment/" + gear, null, 200).path("availableQuantity").asInt(), 1);
  }

  @Test
  public void foreignReferencesCannotCreateLoan() throws Exception {
    Client a = client(), b = client();
    String ownGear = gear(a, 2),
        ownPerson = person(a),
        foreignGear = gear(b, 2),
        foreignPerson = person(b);
    ok(a, "POST", "/loans", loan(foreignGear, ownPerson, 1), 404);
    ok(a, "POST", "/loans", loan(ownGear, foreignPerson, 1), 404);
    assertEquals(
        ok(a, "GET", "/equipment/" + ownGear, null, 200).path("availableQuantity").asInt(), 2);
  }

  @Test
  public void csrfIsRequiredForEveryWrite() throws Exception {
    Client c = client();
    for (String path : List.of("/equipment", "/borrowers", "/loans", "/demo"))
      assertEquals(send(c, "POST", path, "{}", false).statusCode(), 403);
    assertEquals(
        send(c, "DELETE", "/workspace", "{\"confirmation\":\"DELETE\"}", false).statusCode(), 403);
  }

  @Test
  public void workspaceDeletionRequiresConfirmationAndInvalidatesSession() throws Exception {
    Client c = client();
    gear(c, 1);
    ok(c, "DELETE", "/workspace", Map.of("confirmation", "wrong"), 422);
    ok(c, "DELETE", "/workspace", Map.of("confirmation", "DELETE"), 204);
    assertEquals(ok(c, "GET", "/dashboard", null, 200).path("equipmentTypes").asInt(), 0);
    assertEquals(
        send(c, "POST", "/borrowers", "{\"name\":\"After reset\"}", true).statusCode(), 403);
  }

  @Test
  public void exportContainsOwnedDataAndNoSessionMaterial() throws Exception {
    Client c = client();
    ok(c, "POST", "/demo", Map.of(), 200);
    var exported = ok(c, "GET", "/export", null, 200);
    assertEquals(exported.path("equipment").size(), 8);
    assertEquals(exported.path("loans").size(), 9);
    assertFalse(exported.toString().contains("workspace_id"));
    assertFalse(exported.toString().contains("csrfToken"));
  }
}
