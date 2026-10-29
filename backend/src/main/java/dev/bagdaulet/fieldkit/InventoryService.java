package dev.bagdaulet.fieldkit;

import static dev.bagdaulet.fieldkit.Models.*;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
  private final InventoryRepository repo;
  private final WorkspaceService workspaces;
  private final Clock clock;
  private final org.springframework.jdbc.core.JdbcTemplate db;

  public InventoryService(
      InventoryRepository repo,
      WorkspaceService workspaces,
      Clock clock,
      org.springframework.jdbc.core.JdbcTemplate db) {
    this.repo = repo;
    this.workspaces = workspaces;
    this.clock = clock;
    this.db = db;
  }

  @Transactional
  public Equipment saveEquipment(String owner, String id, EquipmentInput input) {
    String name = Inputs.text(input.name(), "Name", 120, true),
        model = Inputs.text(input.model(), "Model", 120, false),
        location = Inputs.text(input.location(), "Location", 120, false),
        description = Inputs.text(input.description(), "Description", 2000, false);
    Category category = Inputs.category(input.category());
    int total = Inputs.quantity(input.totalQuantity());
    workspaces.lock(owner);
    if (id == null) {
      capacity(owner, "equipment", 1000);
      id = UUID.randomUUID().toString();
      db.update(
          "INSERT INTO equipment(id,workspace_id,name,model,category,location,description,total_quantity,available_quantity,created_at) VALUES(?,?,?,?,?,?,?,?,?,?)",
          id,
          owner,
          name,
          model,
          category.name(),
          location,
          description,
          total,
          total,
          Timestamp.from(clock.instant()));
    } else {
      Equipment current = repo.equipment(owner, id, true);
      int onLoan = current.totalQuantity() - current.availableQuantity();
      if (total < onLoan)
        throw ApiException.conflict(
            "STOCK_IN_USE", "Total quantity cannot be lower than the units currently on loan.");
      db.update(
          "UPDATE equipment SET name=?,model=?,category=?,location=?,description=?,total_quantity=?,available_quantity=? WHERE id=? AND workspace_id=?",
          name,
          model,
          category.name(),
          location,
          description,
          total,
          total - onLoan,
          id,
          owner);
    }
    return repo.equipment(owner, id, false);
  }

  @Transactional
  public void deleteEquipment(String owner, String id) {
    workspaces.lock(owner);
    repo.equipment(owner, id, true);
    if (db.queryForObject(
            "SELECT COUNT(*) FROM loans WHERE workspace_id=? AND equipment_id=?",
            Integer.class,
            owner,
            id)
        > 0)
      throw ApiException.conflict(
          "HAS_HISTORY",
          "Keep equipment with loan history so past checkouts remain understandable.");
    db.update("DELETE FROM equipment WHERE id=? AND workspace_id=?", id, owner);
  }

  @Transactional
  public Borrower saveBorrower(String owner, String id, BorrowerInput input) {
    String name = Inputs.text(input.name(), "Name", 100, true),
        team = Inputs.text(input.team(), "Team", 100, false);
    workspaces.lock(owner);
    if (id == null) {
      capacity(owner, "borrowers", 1000);
      id = UUID.randomUUID().toString();
      db.update(
          "INSERT INTO borrowers(id,workspace_id,name,team,created_at) VALUES(?,?,?,?,?)",
          id,
          owner,
          name,
          team,
          Timestamp.from(clock.instant()));
    } else {
      repo.borrower(owner, id);
      db.update(
          "UPDATE borrowers SET name=?,team=? WHERE id=? AND workspace_id=?",
          name,
          team,
          id,
          owner);
    }
    return repo.borrower(owner, id);
  }

  @Transactional
  public void deleteBorrower(String owner, String id) {
    workspaces.lock(owner);
    repo.borrower(owner, id);
    if (db.queryForObject(
            "SELECT COUNT(*) FROM loans WHERE workspace_id=? AND borrower_id=?",
            Integer.class,
            owner,
            id)
        > 0)
      throw ApiException.conflict(
          "HAS_HISTORY", "Keep people with loan history so past checkouts remain understandable.");
    db.update("DELETE FROM borrowers WHERE id=? AND workspace_id=?", id, owner);
  }

  private void capacity(String owner, String table, int max) {
    if (!Set.of("equipment", "borrowers").contains(table)) throw new IllegalArgumentException();
    if (db.queryForObject(
            "SELECT COUNT(*) FROM " + table + " WHERE workspace_id=?", Integer.class, owner)
        >= max)
      throw ApiException.conflict(
          "WORKSPACE_LIMIT",
          "This workspace has reached its record limit. Export and remove older records first.");
  }
}
