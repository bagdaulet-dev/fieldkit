package dev.bagdaulet.fieldkit;

import java.util.*;
import org.springframework.util.MultiValueMap;

public record Queries(String search, String category, String status, int page, int pageSize) {
  public static Queries parse(MultiValueMap<String, String> input, String kind) {
    Set<String> allowed = new HashSet<>(Set.of("search", "page", "pageSize"));
    if (!kind.equals("borrowers")) allowed.add("status");
    if (kind.equals("equipment")) allowed.add("category");
    if (input.keySet().stream().anyMatch(k -> !allowed.contains(k))
        || input.values().stream().anyMatch(v -> v.size() != 1))
      throw ApiException.invalid("Use only the documented query parameters, once each.");
    String search = Inputs.text(input.getFirst("search"), "Search", 100, false),
        category = Objects.requireNonNullElse(input.getFirst("category"), ""),
        status = Objects.requireNonNullElse(input.getFirst("status"), "ALL");
    if (!category.isBlank()) Inputs.category(category);
    Set<String> statuses =
        kind.equals("equipment")
            ? Set.of("ALL", "AVAILABLE", "OUT")
            : Set.of("ALL", "ACTIVE", "OVERDUE", "RETURNED");
    if (!statuses.contains(status)) throw ApiException.invalid("Choose a valid list filter.");
    try {
      int page = Integer.parseInt(Objects.requireNonNullElse(input.getFirst("page"), "1")),
          size = Integer.parseInt(Objects.requireNonNullElse(input.getFirst("pageSize"), "20"));
      if (page < 1 || page > 10000 || size < 1 || size > 50) throw new NumberFormatException();
      return new Queries(search, category, status, page, size);
    } catch (NumberFormatException e) {
      throw ApiException.invalid("Use page 1–10,000 and pageSize 1–50.");
    }
  }
}
