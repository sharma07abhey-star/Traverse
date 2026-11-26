package ticketing.ui;

import java.util.Arrays;
import java.util.List;

import ticketing.exception.TicketingException;
import ticketing.io.CsvImporter;
import ticketing.io.TicketExporter;
import ticketing.model.Booking;
import ticketing.model.TravelClass;
import ticketing.service.ConcurrencySimulation;

/** Runs through every feature automatically: java ticketing.Main --demo */
public class DemoRunner {
    private final AppContext ctx;

    public DemoRunner(AppContext ctx) {
        this.ctx = ctx;
    }

    public void run() throws InterruptedException {
        step("1. Import passengers and trips from CSV (bad rows are reported, good rows imported)");
        System.out.println(CsvImporter.importPassengers("data/passengers.csv", ctx.passengerService));
        System.out.println(CsvImporter.importTrips("data/trips.csv", ctx.tripService));

        step("2. Search");
        ctx.passengerService.search("abhey").forEach(p -> System.out.println("  " + p));
        ctx.tripService.search("delhi", "").forEach(t -> System.out.println("  " + t));

        step("3. Book seats with baggage (overweight bags are charged by class)");
        List<Double> bags = Arrays.asList(18.0, 27.5);
        Booking b1 = ctx.bookingService.bookSeat(1, 1, 12, TravelClass.ECONOMY, bags);
        System.out.println("  " + b1);
        ctx.bookingService.baggageOf(b1.getId()).forEach(g -> System.out.println("     " + g));
        Booking b2 = ctx.bookingService.bookSeat(2, 1, 1, TravelClass.BUSINESS, Arrays.asList(35.0));
        System.out.println("  " + b2);
        Booking b3 = ctx.bookingService.bookSeat(3, 2, 5, TravelClass.FIRST, Arrays.asList(30.0, 44.0, 10.0));
        System.out.println("  " + b3);

        step("4. Double-booking attempt on the same seat is rejected");
        try {
            ctx.bookingService.bookSeat(4, 1, 12, TravelClass.ECONOMY, Arrays.<Double>asList());
        } catch (TicketingException e) {
            System.out.println("  Rejected: " + e.getMessage());
        }

        step("5. Validation: too many bags / bad weight");
        try {
            ctx.bookingService.bookSeat(4, 1, 20, TravelClass.ECONOMY, Arrays.asList(10.0, 10.0, 10.0));
        } catch (TicketingException e) {
            System.out.println("  Rejected: " + e.getMessage());
        }

        step("6. Cancel a booking: seat is freed, baggage is cancelled");
        ctx.bookingService.cancelBooking(b2.getId());
        System.out.println("  " + ctx.bookingService.getBooking(b2.getId()));
        Booking again = ctx.bookingService.bookSeat(4, 1, 1, TravelClass.ECONOMY, Arrays.asList(12.0));
        System.out.println("  Seat 1 re-sold: " + again);

        step("7. Producer-consumer: wait until the loader thread has loaded all checked-in bags");
        ctx.baggageProcessor.awaitIdle();
        ctx.bookingService.allBaggage().forEach(g -> System.out.println("  " + g));

        step("8. Ticket export (file handling)");
        System.out.println("  " + TicketExporter.exportTicket(b1, ctx.passengerService.get(b1.getPassengerId()),
                ctx.tripService.get(b1.getTripId()), ctx.bookingService.baggageOf(b1.getId()), "tickets"));
        System.out.println("  " + TicketExporter.exportBookingsCsv(ctx.bookingService.listBookings(),
                "exports/bookings_export.csv"));

        step("9. Concurrency simulation: 60 threads fight for 10 seats");
        System.out.println(ConcurrencySimulation.run(10, 60));

        step("10. Stream reports");
        System.out.println(ctx.reportService.revenueReport());
        System.out.println(ctx.reportService.occupancyReport());
        System.out.println(ctx.reportService.baggageFeeReport());
        System.out.println(ctx.reportService.topPassengers(0, 3));
        System.out.println(ctx.reportService.heaviestBags(3));
        System.out.println(ctx.reportService.destinations());
    }

    private void step(String title) {
        System.out.println("\n### " + title);
    }
}
