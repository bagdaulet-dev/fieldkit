package dev.bagdaulet.fieldkit;

import static dev.bagdaulet.fieldkit.Models.*;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoService {
  private final WorkspaceService workspaces;
  private final InventoryService inventory;
  private final Clock clock;
  private final org.springframework.jdbc.core.JdbcTemplate db;
  private final boolean enabled;

  public DemoService(
      WorkspaceService workspaces,
      InventoryService inventory,
      Clock clock,
      @Value("${fieldkit.demo-enabled}") boolean enabled,
      org.springframework.jdbc.core.JdbcTemplate db) {
    this.workspaces = workspaces;
    this.inventory = inventory;
    this.clock = clock;
    this.enabled = enabled;
    this.db = db;
  }

  @Transactional
  public void seed(String owner) {
    if (!enabled) throw ApiException.missing();
    workspaces.lock(owner);
    if (db.queryForObject(
                "SELECT COUNT(*) FROM equipment WHERE workspace_id=?", Integer.class, owner)
            > 0
        || db.queryForObject(
                "SELECT COUNT(*) FROM borrowers WHERE workspace_id=?", Integer.class, owner)
            > 0)
      throw ApiException.conflict(
          "WORKSPACE_NOT_EMPTY",
          "The demo needs an empty workspace. Your existing records have been kept.");
    String[][] rows = {
      {"Fujifilm X-T5", "Mirrorless camera", "CAMERAS", "4", "Media cabinet"},
      {"Canon EOS R6", "Full-frame camera", "CAMERAS", "2", "Media cabinet"},
      {"Aputure Amaran", "LED light kit", "LIGHTING", "6", "Studio shelf"},
      {"Zoom H6", "Portable recorder", "AUDIO", "4", "Audio drawer"},
      {"MacBook Pro 14", "M3 Pro · 18 GB", "COMPUTING", "3", "Tech locker"},
      {"Manfrotto Befree", "Travel tripod", "ACCESSORIES", "8", "Studio shelf"},
      {"Wacom Intuos", "Creative pen tablet", "COMPUTING", "3", "Tech locker"},
      {"RØDE Wireless GO", "Wireless microphones", "AUDIO", "5", "Audio drawer"}
    };
    List<String> equipment = new ArrayList<>();
    for (String[] row : rows)
      equipment.add(
          inventory
              .saveEquipment(
                  owner,
                  null,
                  new EquipmentInput(
                      row[0],
                      row[1],
                      row[2],
                      row[4],
                      "A fictional club item for exploring the lending workspace.",
                      Integer.valueOf(row[3])))
              .id());
    String[][] people = {
      {"Alex Morgan", "Film society"},
      {"Maya Chen", "Design studio"},
      {"Jamie Park", "Media club"},
      {"Sam Rivera", "Student radio"},
      {"Noah Williams", "Photography"},
      {"Avery Patel", "Film society"}
    };
    List<String> borrowers = new ArrayList<>();
    for (String[] person : people)
      borrowers.add(
          inventory.saveBorrower(owner, null, new BorrowerInput(person[0], person[1])).id());
    int[] quantities = {2, 1, 2, 1, 1, 2, 1};
    int[] due = {2, 4, -2, 0, 6, 3, -1};
    int[] gear = {0, 1, 2, 3, 4, 5, 7};
    for (int i = 0; i < gear.length; i++) {
      db.update(
          "UPDATE equipment SET available_quantity=available_quantity-? WHERE id=? AND workspace_id=?",
          quantities[i],
          equipment.get(gear[i]),
          owner);
      loan(
          owner,
          equipment.get(gear[i]),
          borrowers.get(i % borrowers.size()),
          quantities[i],
          due[i],
          false,
          i + 1);
    }
    loan(owner, equipment.get(6), borrowers.get(1), 1, -4, true, 8);
    loan(owner, equipment.get(0), borrowers.get(4), 1, -6, true, 10);
    db.update("UPDATE workspaces SET is_demo=TRUE WHERE id=?", owner);
  }

  private void loan(
      String owner,
      String equipment,
      String borrower,
      int quantity,
      int due,
      boolean returned,
      int ago) {
    db.update(
        "INSERT INTO loans(id,workspace_id,equipment_id,borrower_id,quantity,due_date,notes,checked_out_at,returned_at) VALUES(?,?,?,?,?,?,?,?,?)",
        UUID.randomUUID().toString(),
        owner,
        equipment,
        borrower,
        quantity,
        LocalDate.now(clock).plusDays(due),
        "Fictional checkout for a student creative project.",
        Timestamp.from(clock.instant().minus(Duration.ofDays(ago))),
        returned ? Timestamp.from(clock.instant().minus(Duration.ofDays(2))) : null);
  }
}
