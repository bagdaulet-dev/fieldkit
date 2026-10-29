package dev.bagdaulet.fieldkit;

import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class WorkspaceService {
  private final JdbcTemplate db;
  private final TransactionTemplate tx;
  private final Clock clock;

  public WorkspaceService(JdbcTemplate db, TransactionTemplate tx, Clock clock) {
    this.db = db;
    this.tx = tx;
    this.clock = clock;
  }

  public String owner(HttpSession session) {
    String id;
    try {
      id =
          HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256")
                      .digest(session.getId().getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
    if (db.queryForObject("SELECT COUNT(*) FROM workspaces WHERE id=?", Integer.class, id) == 0) {
      try {
        db.update(
            "INSERT INTO workspaces(id,is_demo,created_at,last_seen_at) VALUES(?,FALSE,?,?)",
            id,
            Timestamp.from(clock.instant()),
            Timestamp.from(clock.instant()));
      } catch (DuplicateKeyException ignored) {
        /* Another initial request created this same private workspace. */
      }
    }
    db.update(
        "UPDATE workspaces SET last_seen_at=? WHERE id=?", Timestamp.from(clock.instant()), id);
    return id;
  }

  public void lock(String owner) {
    if (db.queryForList("SELECT id FROM workspaces WHERE id=? FOR UPDATE", String.class, owner)
        .isEmpty()) throw ApiException.missing();
  }

  public boolean demo(String owner) {
    return Boolean.TRUE.equals(
        db.queryForObject("SELECT is_demo FROM workspaces WHERE id=?", Boolean.class, owner));
  }

  public void delete(String owner) {
    tx.executeWithoutResult(
        status -> {
          lock(owner);
          erase(owner);
        });
  }

  private void erase(String owner) {
    db.update("DELETE FROM loans WHERE workspace_id=?", owner);
    db.update("DELETE FROM borrowers WHERE workspace_id=?", owner);
    db.update("DELETE FROM equipment WHERE workspace_id=?", owner);
    db.update("DELETE FROM workspaces WHERE id=?", owner);
  }

  @Scheduled(initialDelay = 60000, fixedDelay = 3600000)
  public void cleanup() {
    Timestamp cutoff = Timestamp.from(clock.instant().minus(Duration.ofDays(7)));
    for (String id :
        db.queryForList(
            "SELECT id FROM workspaces WHERE last_seen_at<? ORDER BY last_seen_at LIMIT 100",
            String.class,
            cutoff))
      tx.executeWithoutResult(
          status -> {
            lock(id);
            if (db.queryForObject(
                    "SELECT COUNT(*) FROM workspaces WHERE id=? AND last_seen_at<?",
                    Integer.class,
                    id,
                    cutoff)
                == 1) erase(id);
          });
  }
}
