package ticketing.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import ticketing.exception.PersistenceException;
import ticketing.model.Baggage;
import ticketing.model.Booking;
import ticketing.model.Passenger;
import ticketing.model.Trip;

/** Writes text files with FileWriter/PrintWriter (character streams). */
public final class TicketExporter {
    private TicketExporter() {
    }

    /** Writes a boarding-pass style ticket to tickets/ticket_<id>.txt and returns the file path. */
    public static String exportTicket(Booking b, Passenger p, Trip t, List<Baggage> bags, String directory) {
        File dir = new File(directory);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new PersistenceException("Cannot create folder " + directory, null);
        }
        File file = new File(dir, "ticket_" + b.getId() + ".txt");
        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            out.println("==================================================");
            out.println("              E-TICKET / BOARDING PASS");
            out.println("==================================================");
            out.printf("Booking ID   : %d (%s)%n", b.getId(), b.getStatus());
            out.printf("Passenger    : %s%n", p.getName());
            out.printf("Passport     : %s%n", p.getPassportNo());
            out.printf("Flight       : %s%n", t.getFlightNo());
            out.printf("Route        : %s -> %s%n", t.getOrigin(), t.getDestination());
            out.printf("Departure    : %s%n", t.getDeparture().format(Trip.DISPLAY));
            out.printf("Seat / Class : %d / %s%n", b.getSeatNo(), b.getTravelClass());
            out.println("--------------------------------------------------");
            out.printf("Baggage (%d bag(s)):%n", bags.size());
            for (Baggage g : bags) {
                out.printf("  Tag %-10s %5.1f kg   extra fee Rs %8.2f   [%s]%n",
                        g.getTagNo(), g.getWeightKg(), g.getFee(), g.getStatus());
            }
            out.println("--------------------------------------------------");
            out.printf("Fare         : Rs %10.2f%n", b.getFare());
            out.printf("Baggage fee  : Rs %10.2f%n", b.getBaggageFee());
            out.printf("TOTAL        : Rs %10.2f%n", b.getTotal());
            out.println("==================================================");
        } catch (IOException e) {
            throw new PersistenceException("Cannot write ticket: " + e.getMessage(), e);
        }
        return file.getPath();
    }

    public static String exportBookingsCsv(List<Booking> bookings, String path) {
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new PersistenceException("Cannot create folder " + parent, null);
        }
        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            out.println("bookingId,passengerId,tripId,seatNo,class,fare,baggageFee,total,status,bookedAt");
            for (Booking b : bookings) {
                out.printf("%d,%d,%d,%d,%s,%.2f,%.2f,%.2f,%s,%s%n", b.getId(), b.getPassengerId(), b.getTripId(),
                        b.getSeatNo(), b.getTravelClass(), b.getFare(), b.getBaggageFee(), b.getTotal(),
                        b.getStatus(), b.getBookedAt());
            }
        } catch (IOException e) {
            throw new PersistenceException("Cannot write CSV: " + e.getMessage(), e);
        }
        return file.getPath();
    }
}
