package ticketing.util;

import ticketing.exception.ValidationException;

/** Central place for input validation so the same rules apply to console input and CSV import. */
public final class Validator {
    private Validator() {
    }

    public static String requireNonBlank(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(field + " must not be empty");
        }
        return value.trim();
    }

    public static String email(String value) {
        String v = requireNonBlank(value, "Email");
        if (!v.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) {
            throw new ValidationException("Invalid email: " + v);
        }
        return v;
    }

    public static String phone(String value) {
        String v = requireNonBlank(value, "Phone");
        if (!v.matches("\\d{10}")) {
            throw new ValidationException("Phone must be exactly 10 digits: " + v);
        }
        return v;
    }

    /** Passport numbers are 6-9 letters/digits; stored in upper case. */
    public static String passport(String value) {
        String v = requireNonBlank(value, "Passport number").toUpperCase();
        if (!v.matches("[A-Z0-9]{6,9}")) {
            throw new ValidationException("Passport must be 6-9 letters/digits: " + v);
        }
        return v;
    }

    public static void inRange(int value, int min, int max, String field) {
        if (value < min || value > max) {
            throw new ValidationException(field + " must be between " + min + " and " + max);
        }
    }

    public static void positive(double value, String field) {
        if (value <= 0) {
            throw new ValidationException(field + " must be greater than 0");
        }
    }
}
