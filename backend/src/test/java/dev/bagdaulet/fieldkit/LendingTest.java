package dev.bagdaulet.fieldkit;

import static org.testng.Assert.*;

import java.util.*;
import java.util.concurrent.*;
import org.testng.annotations.Test;

public class LendingTest extends ApiTestSupport {
  @Test
  public void checkoutReturnAndDoubleReturn() throws Exception {
    Client c = client();
    String gear = gear(c, 3), person = person(c);
    var created = ok(c, "POST", "/loans", loan(gear, person, 2), 201);
    String id = created.path("id").asString();
    assertEquals(created.path("status").asString(), "ACTIVE");
    assertEquals(
        ok(c, "GET", "/equipment/" + gear, null, 200).path("availableQuantity").asInt(), 1);
    assertEquals(
        ok(c, "POST", "/loans/" + id + "/return", Map.of(), 200).path("status").asString(),
        "RETURNED");
    ok(c, "POST", "/loans/" + id + "/return", Map.of(), 409);
    assertEquals(
        ok(c, "GET", "/equipment/" + gear, null, 200).path("availableQuantity").asInt(), 3);
  }

  @Test
  public void unavailableStockDoesNotCreateLoan() throws Exception {
    Client c = client();
    String gear = gear(c, 1), person = person(c);
    ok(c, "POST", "/loans", loan(gear, person, 2), 409);
    assertEquals(ok(c, "GET", "/loans", null, 200).path("totalItems").asInt(), 0);
    assertEquals(
        ok(c, "GET", "/equipment/" + gear, null, 200).path("availableQuantity").asInt(), 1);
  }

  @Test
  public void totalCannotDropBelowCheckedOutUnits() throws Exception {
    Client c = client();
    String gear = gear(c, 3), person = person(c);
    ok(c, "POST", "/loans", loan(gear, person, 2), 201);
    ok(
        c,
        "PUT",
        "/equipment/" + gear,
        Map.of("name", "Updated", "category", "CAMERAS", "totalQuantity", 1),
        409);
    var update =
        ok(
            c,
            "PUT",
            "/equipment/" + gear,
            Map.of("name", "Updated", "category", "CAMERAS", "totalQuantity", 5),
            200);
    assertEquals(update.path("availableQuantity").asInt(), 3);
  }

  @Test
  public void simultaneousLastUnitIsNotBorrowedTwice() throws Exception {
    Client c = client();
    String gear = gear(c, 1), person = person(c);
    String body = json.writeValueAsString(loan(gear, person, 1));
    try (var pool = Executors.newFixedThreadPool(2)) {
      CountDownLatch start = new CountDownLatch(1);
      Callable<Integer> task =
          () -> {
            start.await();
            return send(c, "POST", "/loans", body, true).statusCode();
          };
      var first = pool.submit(task);
      var second = pool.submit(task);
      start.countDown();
      var statuses = new ArrayList<>(List.of(first.get(), second.get()));
      Collections.sort(statuses);
      assertEquals(statuses, List.of(201, 409));
    }
    assertEquals(
        ok(c, "GET", "/equipment/" + gear, null, 200).path("availableQuantity").asInt(), 0);
    assertEquals(ok(c, "GET", "/loans", null, 200).path("totalItems").asInt(), 1);
  }

  @Test
  public void simultaneousReturnsRestoreStockOnce() throws Exception {
    Client c = client();
    String gear = gear(c, 2), person = person(c);
    String id = ok(c, "POST", "/loans", loan(gear, person, 2), 201).path("id").asString();
    try (var pool = Executors.newFixedThreadPool(2)) {
      CountDownLatch start = new CountDownLatch(1);
      Callable<Integer> task =
          () -> {
            start.await();
            return send(c, "POST", "/loans/" + id + "/return", "{}", true).statusCode();
          };
      var first = pool.submit(task);
      var second = pool.submit(task);
      start.countDown();
      var statuses = new ArrayList<>(List.of(first.get(), second.get()));
      Collections.sort(statuses);
      assertEquals(statuses, List.of(200, 409));
    }
    assertEquals(
        ok(c, "GET", "/equipment/" + gear, null, 200).path("availableQuantity").asInt(), 2);
  }

  @Test
  public void historyPreventsEquipmentAndBorrowerDeletion() throws Exception {
    Client c = client();
    String gear = gear(c, 1), person = person(c);
    String loan = ok(c, "POST", "/loans", loan(gear, person, 1), 201).path("id").asString();
    ok(c, "DELETE", "/equipment/" + gear, null, 409);
    ok(c, "DELETE", "/borrowers/" + person, null, 409);
    ok(c, "POST", "/loans/" + loan + "/return", Map.of(), 200);
    ok(c, "DELETE", "/equipment/" + gear, null, 409);
    ok(c, "DELETE", "/borrowers/" + person, null, 409);
  }

  @Test
  public void unusedRecordsCanBeEditedAndDeleted() throws Exception {
    Client c = client();
    String gear = gear(c, 1), person = person(c);
    assertEquals(
        ok(
                c,
                "PUT",
                "/borrowers/" + person,
                Map.of("name", "Updated name", "team", "New team"),
                200)
            .path("name")
            .asString(),
        "Updated name");
    ok(c, "DELETE", "/borrowers/" + person, null, 204);
    ok(c, "DELETE", "/equipment/" + gear, null, 204);
    ok(c, "GET", "/equipment/" + gear, null, 404);
  }

  @Test
  public void demoStatisticsReconcileWithInventory() throws Exception {
    Client c = client();
    ok(c, "POST", "/demo", Map.of(), 200);
    var d = ok(c, "GET", "/dashboard", null, 200);
    assertEquals(d.path("equipmentTypes").asInt(), 8);
    assertEquals(d.path("totalUnits").asInt(), 35);
    assertEquals(d.path("availableUnits").asInt(), 25);
    assertEquals(d.path("activeLoans").asInt(), 7);
    assertEquals(d.path("overdueLoans").asInt(), 2);
    assertEquals(d.path("returnedLoans").asInt(), 2);
    assertEquals(ok(c, "GET", "/loans?status=OVERDUE", null, 200).path("totalItems").asInt(), 2);
    assertEquals(ok(c, "GET", "/loans?status=ACTIVE", null, 200).path("totalItems").asInt(), 7);
    assertEquals(ok(c, "GET", "/loans?status=RETURNED", null, 200).path("totalItems").asInt(), 2);
    ok(c, "POST", "/demo", Map.of(), 409);
  }

  @Test
  public void failedLoanInsertRollsBackStockChange() throws Exception {
    Client c = client();
    String gear = gear(c, 2), person = person(c);
    var db = app.getBean(org.springframework.jdbc.core.JdbcTemplate.class);
    db.execute(
        "ALTER TABLE loans ADD CONSTRAINT fieldkit_test_reject_note CHECK (notes <> 'simulate-rollback')");
    try {
      var body = new HashMap<>(loan(gear, person, 1));
      body.put("notes", "simulate-rollback");
      ok(c, "POST", "/loans", body, 503);
      assertEquals(
          ok(c, "GET", "/equipment/" + gear, null, 200).path("availableQuantity").asInt(), 2);
      assertEquals(ok(c, "GET", "/loans", null, 200).path("totalItems").asInt(), 0);
    } finally {
      boolean mysql = System.getenv().getOrDefault("TEST_STORAGE", "preview").equals("mysql");
      db.execute(
          "ALTER TABLE loans DROP "
              + (mysql ? "CHECK " : "CONSTRAINT ")
              + "fieldkit_test_reject_note");
    }
  }
}
