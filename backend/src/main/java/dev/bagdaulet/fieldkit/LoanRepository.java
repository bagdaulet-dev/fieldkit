package dev.bagdaulet.fieldkit;

import static dev.bagdaulet.fieldkit.Models.*;

import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;

@Repository
public class LoanRepository {
  private final JdbcTemplate db;
  private final Clock clock;
  private static final String SELECT =
      "SELECT l.*,e.name equipment_name,e.model equipment_model,e.category,b.name borrower_name,b.team borrower_team FROM loans l JOIN equipment e ON e.id=l.equipment_id AND e.workspace_id=l.workspace_id JOIN borrowers b ON b.id=l.borrower_id AND b.workspace_id=l.workspace_id";

  public LoanRepository(JdbcTemplate db, Clock clock) {
    this.db = db;
    this.clock = clock;
  }

  final RowMapper<Loan> mapper = this::map;

  private Loan map(java.sql.ResultSet r, int n) throws java.sql.SQLException {
    var returned = r.getTimestamp("returned_at");
    LocalDate due = r.getDate("due_date").toLocalDate();
    return new Loan(
        r.getString("id"),
        r.getString("equipment_id"),
        r.getString("equipment_name"),
        r.getString("equipment_model"),
        Category.valueOf(r.getString("category")),
        r.getString("borrower_id"),
        r.getString("borrower_name"),
        r.getString("borrower_team"),
        r.getInt("quantity"),
        due,
        r.getString("notes"),
        r.getTimestamp("checked_out_at").toInstant(),
        returned == null ? null : returned.toInstant(),
        returned != null ? "RETURNED" : due.isBefore(LocalDate.now(clock)) ? "OVERDUE" : "ACTIVE");
  }

  public Loan find(String owner, String id, boolean lock) {
    var rows =
        db.query(
            SELECT + " WHERE l.workspace_id=? AND l.id=?" + (lock ? " FOR UPDATE" : ""),
            mapper,
            owner,
            id);
    if (rows.isEmpty()) throw ApiException.missing();
    return rows.getFirst();
  }

  public Page<Loan> page(String owner, Queries query) {
    String where = " WHERE l.workspace_id=?";
    List<Object> args = new ArrayList<>(List.of(owner));
    if (!query.search().isBlank()) {
      where += " AND (LOWER(e.name) LIKE ? ESCAPE '!' OR LOWER(b.name) LIKE ? ESCAPE '!')";
      args.add(InventoryRepository.like(query.search()));
      args.add(InventoryRepository.like(query.search()));
    }
    switch (query.status()) {
      case "ACTIVE" -> where += " AND l.returned_at IS NULL";
      case "OVERDUE" -> {
        where += " AND l.returned_at IS NULL AND l.due_date<?";
        args.add(LocalDate.now(clock));
      }
      case "RETURNED" -> where += " AND l.returned_at IS NOT NULL";
      default -> {}
    }
    long total =
        db.queryForObject(
            "SELECT COUNT(*) FROM (" + SELECT + where + ") matches", Long.class, args.toArray());
    args.add(query.pageSize());
    args.add((query.page() - 1) * query.pageSize());
    return InventoryRepository.page(
        db.query(
            SELECT
                + where
                + " ORDER BY CASE WHEN l.returned_at IS NULL THEN 0 ELSE 1 END,l.due_date,l.checked_out_at DESC,l.id LIMIT ? OFFSET ?",
            mapper,
            args.toArray()),
        query,
        total);
  }
}
