package dev.bagdaulet.fieldkit;

import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
  private final JdbcTemplate db;
  private final InventoryRepository inventory;
  private final LoanRepository loans;
  private final Clock clock;

  public DashboardService(
      JdbcTemplate db, InventoryRepository inventory, LoanRepository loans, Clock clock) {
    this.db = db;
    this.inventory = inventory;
    this.loans = loans;
    this.clock = clock;
  }

  public Map<String, Object> dashboard(String owner) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put(
        "equipmentTypes", count("SELECT COUNT(*) FROM equipment WHERE workspace_id=?", owner));
    result.put(
        "totalUnits",
        count("SELECT COALESCE(SUM(total_quantity),0) FROM equipment WHERE workspace_id=?", owner));
    result.put(
        "availableUnits",
        count(
            "SELECT COALESCE(SUM(available_quantity),0) FROM equipment WHERE workspace_id=?",
            owner));
    result.put(
        "activeLoans",
        count("SELECT COUNT(*) FROM loans WHERE workspace_id=? AND returned_at IS NULL", owner));
    result.put(
        "overdueLoans",
        count(
            "SELECT COUNT(*) FROM loans WHERE workspace_id=? AND returned_at IS NULL AND due_date<?",
            owner,
            LocalDate.now(clock)));
    result.put("borrowers", count("SELECT COUNT(*) FROM borrowers WHERE workspace_id=?", owner));
    result.put(
        "returnedLoans",
        count(
            "SELECT COUNT(*) FROM loans WHERE workspace_id=? AND returned_at IS NOT NULL", owner));
    result.put("recentLoans", loans.page(owner, new Queries("", "", "ACTIVE", 1, 6)).items());
    result.put(
        "featuredEquipment",
        inventory.equipmentPage(owner, new Queries("", "", "ALL", 1, 4)).items());
    result.put(
        "categories",
        db.query(
            "SELECT category,COUNT(*) types,COALESCE(SUM(total_quantity),0) total,COALESCE(SUM(available_quantity),0) available FROM equipment WHERE workspace_id=? GROUP BY category ORDER BY category",
            (r, n) ->
                Map.of(
                    "category",
                    r.getString("category"),
                    "types",
                    r.getInt("types"),
                    "total",
                    r.getInt("total"),
                    "available",
                    r.getInt("available")),
            owner));
    return result;
  }

  private long count(String sql, Object... args) {
    return Objects.requireNonNull(db.queryForObject(sql, Long.class, args));
  }

  public Map<String, Object> export(String owner) {
    return Map.of(
        "exportedAt",
        clock.instant(),
        "equipment",
        inventory.equipmentPage(owner, new Queries("", "", "ALL", 1, 1000)).items(),
        "borrowers",
        inventory.borrowerPage(owner, new Queries("", "", "ALL", 1, 1000)).items(),
        "loans",
        loans.page(owner, new Queries("", "", "ALL", 1, 2000)).items());
  }
}
