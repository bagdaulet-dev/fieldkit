package dev.bagdaulet.fieldkit;

import java.time.*;
import java.util.*;

public final class Models {
  private Models() {}

  public enum Category {
    CAMERAS,
    AUDIO,
    LIGHTING,
    COMPUTING,
    ACCESSORIES
  }

  public record Equipment(
      String id,
      String name,
      String model,
      Category category,
      String location,
      String description,
      int totalQuantity,
      int availableQuantity,
      Instant createdAt) {}

  public record Borrower(String id, String name, String team, int activeLoans, Instant createdAt) {}

  public record Loan(
      String id,
      String equipmentId,
      String equipmentName,
      String equipmentModel,
      Category category,
      String borrowerId,
      String borrowerName,
      String borrowerTeam,
      int quantity,
      LocalDate dueDate,
      String notes,
      Instant checkedOutAt,
      Instant returnedAt,
      String status) {}

  public record Page<T>(List<T> items, int page, int pageSize, long totalItems, int totalPages) {}

  public record EquipmentInput(
      String name,
      String model,
      String category,
      String location,
      String description,
      Integer totalQuantity) {}

  public record BorrowerInput(String name, String team) {}

  public record LoanInput(
      String equipmentId, String borrowerId, Integer quantity, String dueDate, String notes) {}

  public record DeleteInput(String confirmation) {}

  public record EmptyInput() {}
}
