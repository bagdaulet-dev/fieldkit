package dev.bagdaulet.fieldkit;

import java.util.*;

public final class Inputs {
  private Inputs() {}

  public static String text(String value, String field, int max, boolean required) {
    if (value == null) {
      if (required) throw ApiException.invalid(field + " is required.");
      return "";
    }
    String result = value.strip();
    if ((required && result.isEmpty())
        || result.length() > max
        || result
            .codePoints()
            .anyMatch(
                c ->
                    Character.isISOControl(c)
                            && !(field.equals("Notes") || field.equals("Description"))
                        || Character.getType(c) == Character.SURROGATE))
      throw ApiException.invalid(
          "Check "
              + field.toLowerCase()
              + ": use "
              + (required ? "1–" : "up to ")
              + max
              + " readable characters.");
    if (result
        .codePoints()
        .anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t'))
      throw ApiException.invalid("Remove control characters from " + field.toLowerCase() + ".");
    return result.replace("\r\n", "\n");
  }

  public static int quantity(Integer value) {
    if (value == null || value < 1 || value > 1000)
      throw ApiException.invalid("Quantity must be a whole number from 1 to 1,000.");
    return value;
  }

  public static String id(String value) {
    try {
      return UUID.fromString(value).toString();
    } catch (Exception e) {
      throw ApiException.missing();
    }
  }

  public static Models.Category category(String value) {
    try {
      return Models.Category.valueOf(value);
    } catch (Exception e) {
      throw ApiException.invalid("Choose a valid equipment category.");
    }
  }
}
