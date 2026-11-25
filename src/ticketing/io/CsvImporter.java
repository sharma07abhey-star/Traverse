package ticketing.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import ticketing.exception.PersistenceException;
import ticketing.exception.TicketingException;
import ticketing.service.PassengerService;
import ticketing.service.TripService;
import ticketing.model.Trip;

/**
 * Reads CSV files with FileReader + BufferedReader (character streams).
 * Bad lines are reported and skipped; good lines are imported. try-with-resources always closes the file.
 *
 * passengers.csv : name,email,phone,passportNo
 * trips.csv      : flightNo,origin,destination,departure(yyyy-MM-dd HH:mm),totalSeats,baseFare
 */
public final class CsvImporter {
    private CsvImporter() {
    }

    public static ImportResult importPassengers(String path, PassengerService service) {
        ImportResult result = new ImportResult();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty() || (lineNo == 1 && line.toLowerCase().startsWith("name"))) {
                    continue;                                   // blank line or header
                }
                String[] f = line.split(",", -1);
                try {
                    if (f.length != 4) {
                        throw new TicketingException("expected 4 columns but found " + f.length);
                    }
                    service.add(f[0], f[1], f[2], f[3]);
                    result.success();
                } catch (TicketingException e) {
                    result.error(lineNo, e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new PersistenceException("Cannot read file '" + path + "': " + e.getMessage(), e);
        }
        return result;
    }

    public static ImportResult importTrips(String path, TripService service) {
        ImportResult result = new ImportResult();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty() || (lineNo == 1 && line.toLowerCase().startsWith("flight"))) {
                    continue;
                }
                String[] f = line.split(",", -1);
                try {
                    if (f.length != 6) {
                        throw new TicketingException("expected 6 columns but found " + f.length);
                    }
                    LocalDateTime departure = LocalDateTime.parse(f[3].trim(), Trip.DISPLAY);
                    int seats = Integer.parseInt(f[4].trim());
                    double fare = Double.parseDouble(f[5].trim());
                    service.add(f[0], f[1], f[2], departure, seats, fare);
                    result.success();
                } catch (DateTimeParseException e) {
                    result.error(lineNo, "bad date/time '" + f[3].trim() + "' (use yyyy-MM-dd HH:mm)");
                } catch (NumberFormatException e) {
                    result.error(lineNo, "seats/fare must be numbers");
                } catch (TicketingException e) {
                    result.error(lineNo, e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new PersistenceException("Cannot read file '" + path + "': " + e.getMessage(), e);
        }
        return result;
    }
}
