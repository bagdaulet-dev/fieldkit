package dev.bagdaulet.fieldkit;

import static dev.bagdaulet.fieldkit.Models.*;

import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;

@Repository
public class InventoryRepository {
  private final JdbcTemplate db;

  public InventoryRepository(JdbcTemplate db) {
    this.db = db;
  }

  final RowMapper<Equipment> equipmentMapper =
      (r, n) ->
          new Equipment(
              r.getString("id"),
              r.getString("name"),
              r.getString("model"),
              Category.valueOf(r.getString("category")),
              r.getString("location"),
              r.getString("description"),
              r.getInt("total_quantity"),
              r.getInt("available_quantity"),
              r.getTimestamp("created_at").toInstant());
  final RowMapper<Borrower> borrowerMapper =
      (r, n) ->
          new Borrower(
              r.getString("id"),
              r.getString("name"),
              r.getString("team"),
              r.getInt("active_loans"),
              r.getTimestamp("created_at").toInstant());

  public Equipment equipment(String owner, String id, boolean lock) {
    var rows =
        db.query(
            "SELECT * FROM equipment WHERE workspace_id=? AND id=?" + (lock ? " FOR UPDATE" : ""),
            equipmentMapper,
            owner,
            id);
    if (rows.isEmpty()) throw ApiException.missing();
    return rows.getFirst();
  }

  public Borrower borrower(String owner, String id) {
    var rows =
        db.query(
            "SELECT b.*, (SELECT COUNT(*) FROM loans l WHERE l.borrower_id=b.id AND l.workspace_id=b.workspace_id AND l.returned_at IS NULL) active_loans FROM borrowers b WHERE b.workspace_id=? AND b.id=?",
            borrowerMapper,
            owner,
            id);
    if (rows.isEmpty()) throw ApiException.missing();
    return rows.getFirst();
  }

  public Page<Equipment> equipmentPage(String owner, Queries query) {
    String where = " WHERE workspace_id=?";
    List<Object> args = new ArrayList<>(List.of(owner));
    if (!query.search().isBlank()) {
      where += " AND (LOWER(name) LIKE ? ESCAPE '!' OR LOWER(model) LIKE ? ESCAPE '!')";
      args.add(like(query.search()));
      args.add(like(query.search()));
    }
    if (!query.category().isBlank()) {
      where += " AND category=?";
      args.add(query.category());
    }
    if (query.status().equals("AVAILABLE")) where += " AND available_quantity>0";
    if (query.status().equals("OUT")) where += " AND available_quantity=0";
    long total =
        db.queryForObject("SELECT COUNT(*) FROM equipment" + where, Long.class, args.toArray());
    args.add(query.pageSize());
    args.add((query.page() - 1) * query.pageSize());
    return page(
        db.query(
            "SELECT * FROM equipment" + where + " ORDER BY name,id LIMIT ? OFFSET ?",
            equipmentMapper,
            args.toArray()),
        query,
        total);
  }

  public Page<Borrower> borrowerPage(String owner, Queries query) {
    String where = " WHERE b.workspace_id=?";
    List<Object> args = new ArrayList<>(List.of(owner));
    if (!query.search().isBlank()) {
      where += " AND (LOWER(b.name) LIKE ? ESCAPE '!' OR LOWER(b.team) LIKE ? ESCAPE '!')";
      args.add(like(query.search()));
      args.add(like(query.search()));
    }
    long total =
        db.queryForObject("SELECT COUNT(*) FROM borrowers b" + where, Long.class, args.toArray());
    args.add(query.pageSize());
    args.add((query.page() - 1) * query.pageSize());
    return page(
        db.query(
            "SELECT b.*, (SELECT COUNT(*) FROM loans l WHERE l.borrower_id=b.id AND l.workspace_id=b.workspace_id AND l.returned_at IS NULL) active_loans FROM borrowers b"
                + where
                + " ORDER BY b.name,b.id LIMIT ? OFFSET ?",
            borrowerMapper,
            args.toArray()),
        query,
        total);
  }

  static String like(String value) {
    return "%"
        + value.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_")
        + "%";
  }

  static <T> Page<T> page(List<T> items, Queries query, long total) {
    return new Page<>(
        items,
        query.page(),
        query.pageSize(),
        total,
        (int) ((total + query.pageSize() - 1) / query.pageSize()));
  }
}
