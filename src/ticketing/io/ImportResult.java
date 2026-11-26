package ticketing.io;

import java.util.ArrayList;
import java.util.List;

/** Outcome of a CSV import: how many rows were imported and which lines were rejected (and why). */
public class ImportResult {
    private int imported = 0;
    private final List<String> errors = new ArrayList<>();

    public void success() {
        imported++;
    }

    public void error(int lineNo, String message) {
        errors.add("line " + lineNo + ": " + message);
    }

    public int getImported() {
        return imported;
    }

    public List<String> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Imported " + imported + " row(s), rejected " + errors.size());
        errors.forEach(e -> sb.append("\n   ! ").append(e));
        return sb.toString();
    }
}
