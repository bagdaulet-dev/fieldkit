package dev.bagdaulet.fieldkit;

import static dev.bagdaulet.fieldkit.Models.*;

import jakarta.servlet.http.HttpSession;
import java.util.*;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiController {
  private final WorkspaceService workspaces;
  private final InventoryRepository inventory;
  private final InventoryService stock;
  private final LoanRepository loans;
  private final LoanService lending;
  private final DashboardService dashboard;
  private final DemoService demo;
  private final Environment env;
  private final JdbcTemplate db;

  public ApiController(
      WorkspaceService workspaces,
      InventoryRepository inventory,
      InventoryService stock,
      LoanRepository loans,
      LoanService lending,
      DashboardService dashboard,
      DemoService demo,
      Environment env,
      JdbcTemplate db) {
    this.workspaces = workspaces;
    this.inventory = inventory;
    this.stock = stock;
    this.loans = loans;
    this.lending = lending;
    this.dashboard = dashboard;
    this.demo = demo;
    this.env = env;
    this.db = db;
  }

  @GetMapping("/health")
  public Map<String, String> health() {
    db.queryForObject("SELECT 1", Integer.class);
    return Map.of("status", "ok");
  }

  @GetMapping("/session")
  public Map<String, Object> session(HttpSession session, CsrfToken token) {
    String owner = workspaces.owner(session);
    return Map.of(
        "csrfToken",
        token.getToken(),
        "mode",
        env.matchesProfiles("mysql") ? "mysql" : "preview",
        "isDemo",
        workspaces.demo(owner),
        "demoEnabled",
        env.getProperty("fieldkit.demo-enabled", Boolean.class, true));
  }

  @PostMapping("/demo")
  public Map<String, Object> seed(
      HttpSession session, CsrfToken token, @RequestBody EmptyInput empty) {
    demo.seed(workspaces.owner(session));
    return session(session, token);
  }

  @GetMapping("/dashboard")
  public Map<String, Object> dashboard(HttpSession session) {
    return dashboard.dashboard(workspaces.owner(session));
  }

  @GetMapping("/export")
  public Map<String, Object> export(HttpSession session) {
    return dashboard.export(workspaces.owner(session));
  }

  @DeleteMapping("/workspace")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteWorkspace(HttpSession session, @RequestBody DeleteInput input) {
    if (!"DELETE".equals(input.confirmation()))
      throw ApiException.invalid("Type DELETE to confirm workspace deletion.");
    workspaces.delete(workspaces.owner(session));
    session.invalidate();
  }

  @GetMapping("/equipment")
  public Page<Equipment> equipment(
      HttpSession s, @RequestParam MultiValueMap<String, String> query) {
    return inventory.equipmentPage(workspaces.owner(s), Queries.parse(query, "equipment"));
  }

  @PostMapping("/equipment")
  @ResponseStatus(HttpStatus.CREATED)
  public Equipment addEquipment(HttpSession s, @RequestBody EquipmentInput input) {
    return stock.saveEquipment(workspaces.owner(s), null, input);
  }

  @GetMapping("/equipment/{id}")
  public Equipment equipment(HttpSession s, @PathVariable UUID id) {
    return inventory.equipment(workspaces.owner(s), id.toString(), false);
  }

  @PutMapping("/equipment/{id}")
  public Equipment editEquipment(
      HttpSession s, @PathVariable UUID id, @RequestBody EquipmentInput input) {
    return stock.saveEquipment(workspaces.owner(s), id.toString(), input);
  }

  @DeleteMapping("/equipment/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteEquipment(HttpSession s, @PathVariable UUID id) {
    stock.deleteEquipment(workspaces.owner(s), id.toString());
  }

  @GetMapping("/borrowers")
  public Page<Borrower> borrowers(
      HttpSession s, @RequestParam MultiValueMap<String, String> query) {
    return inventory.borrowerPage(workspaces.owner(s), Queries.parse(query, "borrowers"));
  }

  @PostMapping("/borrowers")
  @ResponseStatus(HttpStatus.CREATED)
  public Borrower addBorrower(HttpSession s, @RequestBody BorrowerInput input) {
    return stock.saveBorrower(workspaces.owner(s), null, input);
  }

  @PutMapping("/borrowers/{id}")
  public Borrower editBorrower(
      HttpSession s, @PathVariable UUID id, @RequestBody BorrowerInput input) {
    return stock.saveBorrower(workspaces.owner(s), id.toString(), input);
  }

  @DeleteMapping("/borrowers/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteBorrower(HttpSession s, @PathVariable UUID id) {
    stock.deleteBorrower(workspaces.owner(s), id.toString());
  }

  @GetMapping("/loans")
  public Page<Loan> loans(HttpSession s, @RequestParam MultiValueMap<String, String> query) {
    return loans.page(workspaces.owner(s), Queries.parse(query, "loans"));
  }

  @PostMapping("/loans")
  @ResponseStatus(HttpStatus.CREATED)
  public Loan checkout(HttpSession s, @RequestBody LoanInput input) {
    return lending.checkout(workspaces.owner(s), input);
  }

  @GetMapping("/loans/{id}")
  public Loan loan(HttpSession s, @PathVariable UUID id) {
    return loans.find(workspaces.owner(s), id.toString(), false);
  }

  @PostMapping("/loans/{id}/return")
  public Loan returnLoan(HttpSession s, @PathVariable UUID id, @RequestBody EmptyInput input) {
    return lending.returnLoan(workspaces.owner(s), id.toString());
  }
}
