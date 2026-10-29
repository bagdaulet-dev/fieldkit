package dev.bagdaulet.fieldkit;

import static dev.bagdaulet.fieldkit.Models.*;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanService {
  private final LoanRepository loans;
  private final InventoryRepository inventory;
  private final WorkspaceService workspaces;
  private final Clock clock;
  private final org.springframework.jdbc.core.JdbcTemplate db;

  public LoanService(
      LoanRepository loans,
      InventoryRepository inventory,
      WorkspaceService workspaces,
      Clock clock,
      org.springframework.jdbc.core.JdbcTemplate db) {
    this.loans = loans;
    this.inventory = inventory;
    this.workspaces = workspaces;
    this.clock = clock;
    this.db = db;
  }

  @Transactional
  public Loan checkout(String owner, LoanInput input) {
    String equipmentId = Inputs.id(input.equipmentId()), borrowerId = Inputs.id(input.borrowerId());
    int quantity = Inputs.quantity(input.quantity());
    String notes = Inputs.text(input.notes(), "Notes", 2000, false);
    LocalDate due;
    try {
      due = LocalDate.parse(input.dueDate());
    } catch (Exception e) {
      throw ApiException.invalid("Choose a valid due date.");
    }
    if (due.isBefore(LocalDate.now(clock)) || due.isAfter(LocalDate.now(clock).plusYears(1)))
      throw ApiException.invalid("Choose a due date from today through one year from today.");
    workspaces.lock(owner);
    Equipment equipment = inventory.equipment(owner, equipmentId, true);
    inventory.borrower(owner, borrowerId);
    if (equipment.availableQuantity() < quantity)
      throw ApiException.conflict(
          "INSUFFICIENT_STOCK",
          "There are not enough available units. Refresh availability or reduce the quantity.");
    if (db.queryForObject("SELECT COUNT(*) FROM loans WHERE workspace_id=?", Integer.class, owner)
        >= 2000)
      throw ApiException.conflict(
          "WORKSPACE_LIMIT",
          "Keep up to 2,000 loans in this workspace. Export your history before starting a new workspace.");
    String id = UUID.randomUUID().toString();
    db.update(
        "UPDATE equipment SET available_quantity=available_quantity-? WHERE id=? AND workspace_id=?",
        quantity,
        equipmentId,
        owner);
    db.update(
        "INSERT INTO loans(id,workspace_id,equipment_id,borrower_id,quantity,due_date,notes,checked_out_at) VALUES(?,?,?,?,?,?,?,?)",
        id,
        owner,
        equipmentId,
        borrowerId,
        quantity,
        due,
        notes,
        Timestamp.from(clock.instant()));
    return loans.find(owner, id, false);
  }

  @Transactional
  public Loan returnLoan(String owner, String id) {
    workspaces.lock(owner);
    Loan loan = loans.find(owner, id, true);
    if (loan.returnedAt() != null)
      throw ApiException.conflict(
          "ALREADY_RETURNED", "This loan has already been returned. Stock has not been changed.");
    inventory.equipment(owner, loan.equipmentId(), true);
    db.update(
        "UPDATE loans SET returned_at=? WHERE id=? AND workspace_id=?",
        Timestamp.from(clock.instant()),
        id,
        owner);
    db.update(
        "UPDATE equipment SET available_quantity=available_quantity+? WHERE id=? AND workspace_id=?",
        loan.quantity(),
        loan.equipmentId(),
        owner);
    return loans.find(owner, id, false);
  }
}
