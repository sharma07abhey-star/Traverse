package ticketing.ui;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import ticketing.exception.ValidationException;
import ticketing.model.TravelClass;
import ticketing.model.Trip;

/** Console input helper: every read method loops until the user types something valid. */
public class InputReader {
    private final Scanner scanner = new Scanner(System.in);

    public String text(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();           // NoSuchElementException on end-of-input => app exits
    }

    public String requiredText(String prompt) {
        while (true) {
            String s = text(prompt);
            if (!s.isEmpty()) {
                return s;
            }
            System.out.println("  ! This field cannot be empty.");
        }
    }

    public int integer(String prompt, int min, int max) {
        while (true) {
            String s = text(prompt);
            try {
                int v = Integer.parseInt(s);
                if (v >= min && v <= max) {
                    return v;
                }
                System.out.println("  ! Enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("  ! Please enter a whole number.");
            }
        }
    }

    public int integer(String prompt) {
        return integer(prompt, 1, Integer.MAX_VALUE);
    }

    public double decimal(String prompt) {
        while (true) {
            String s = text(prompt);
            try {
                double v = Double.parseDouble(s);
                if (v > 0) {
                    return v;
                }
                System.out.println("  ! Value must be greater than 0.");
            } catch (NumberFormatException e) {
                System.out.println("  ! Please enter a number.");
            }
        }
    }

    public LocalDateTime dateTime(String prompt) {
        while (true) {
            String s = text(prompt + " (yyyy-MM-dd HH:mm): ");
            try {
                return LocalDateTime.parse(s, Trip.DISPLAY);
            } catch (DateTimeParseException e) {
                System.out.println("  ! Use the format 2027-03-15 14:30");
            }
        }
    }

    public boolean yesNo(String prompt) {
        while (true) {
            String s = text(prompt + " (y/n): ").toLowerCase();
            if (s.equals("y") || s.equals("yes")) {
                return true;
            }
            if (s.equals("n") || s.equals("no")) {
                return false;
            }
        }
    }

    public TravelClass travelClass() {
        System.out.println("  1) ECONOMY   2) BUSINESS   3) FIRST");
        int c = integer("  Travel class: ", 1, 3);
        return TravelClass.values()[c - 1];
    }

    /**
     * Reads bag weights typed on one line, e.g. "18.5 23 9". Uses StreamTokenizer to split the
     * line into numbers. An empty line means no baggage.
     */
    public List<Double> weights(String prompt) {
        while (true) {
            String line = text(prompt);
            try {
                return parseWeights(line);
            } catch (ValidationException e) {
                System.out.println("  ! " + e.getMessage());
            }
        }
    }

    public static List<Double> parseWeights(String line) {
        List<Double> weights = new ArrayList<>();
        StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(line));
        try {
            while (tokenizer.nextToken() != StreamTokenizer.TT_EOF) {
                if (tokenizer.ttype == StreamTokenizer.TT_NUMBER) {
                    weights.add(tokenizer.nval);
                } else {
                    throw new ValidationException("Weights must be numbers separated by spaces");
                }
            }
        } catch (IOException e) {
            throw new ValidationException("Could not read weights: " + e.getMessage());
        }
        return weights;
    }
}
