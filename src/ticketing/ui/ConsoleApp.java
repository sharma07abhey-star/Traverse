package ticketing.ui;

import java.util.List;
import java.util.stream.Collectors;

import ticketing.concurrent.BaggageProcessor;
import ticketing.exception.TicketingException;
import ticketing.io.CsvImporter;
import ticketing.io.FileBackup;
import ticketing.io.TicketExporter;
import ticketing.model.Baggage;
import ticketing.model.Booking;
import ticketing.model.Passenger;
import ticketing.model.TravelClass;
import ticketing.model.Trip;
import ticketing.service.ConcurrencySimulation;

/** The menu-driven console user interface. Every action is wrapped so a bad input never crashes the app. */
public class ConsoleApp {
    private final AppContext ctx;
    private final InputReader in = new InputReader();

    public ConsoleApp(AppContext ctx) {
        this.ctx = ctx;
    }

    public void run() {
        System.out.println("============================================");
        System.out.println("   Flight Booking & Baggage System v1.0");
        System.out.println("   Made by: Abhey Sharma, Moksh Sharma, Janit, Harshit");
        System.out.println("============================================");
        System.out.println("Storage mode: " + ctx.mode);
        while (true) {
            System.out.println("\n----------- MAIN MENU -----------");
            System.out.println(" 1. Passenger management");
            System.out.println(" 2. Trip management");
            System.out.println(" 3. Bookings (seat + baggage)");
            System.out.println(" 4. Baggage tracking");
            System.out.println(" 5. Reports (Streams)");
            System.out.println(" 6. File operations (CSV import / export / backup)");
            System.out.println(" 7. Concurrency simulation (double-booking test)");
            System.out.println(" 0. Exit");
            int choice = in.integer("Choice: ", 0, 7);
            if (choice == 0) {
                return;
            }
            try {
                switch (choice) {
                    case 1:
                        passengerMenu();
                        break;
                    case 2:
                        tripMenu();
                        break;
                    case 3:
                        bookingMenu();
                        break;
                    case 4:
                        baggageMenu();
                        break;
                    case 5:
                        reportMenu();
                        break;
                    case 6:
                        fileMenu();
                        break;
                    case 7:
                        simulation();
                        break;
                    default:
                        break;
                }
            } catch (TicketingException e) {
                System.out.println("  ! " + e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------ passengers

    private void passengerMenu() {
        while (true) {
            System.out.println("\n-- Passengers --  1.Add 2.List 3.Search 4.Update 5.Delete 0.Back");
            int c = in.integer("Choice: ", 0, 5);
            if (c == 0) {
                return;
            }
            try {
                switch (c) {
                    case 1:
                        Passenger p = ctx.passengerService.add(in.requiredText("Name: "), in.requiredText("Email: "),
                                in.requiredText("Phone (10 digits): "), in.requiredText("Passport no: "));
                        System.out.println("  Added passenger #" + p.getId());
                        break;
                    case 2:
                        printList(ctx.passengerService.list());
                        break;
                    case 3:
                        printList(ctx.passengerService.search(in.requiredText("Search (name/email/passport): ")));
                        break;
                    case 4:
                        int id = in.integer("Passenger id: ");
                        Passenger old = ctx.passengerService.get(id);
                        System.out.println("  Current: " + old);
                        ctx.passengerService.update(id, in.requiredText("New name: "), in.requiredText("New email: "),
                                in.requiredText("New phone: "));
                        System.out.println("  Updated.");
                        break;
                    case 5:
                        ctx.passengerService.delete(in.integer("Passenger id: "));
                        System.out.println("  Deleted.");
                        break;
                    default:
                        break;
                }
            } catch (TicketingException e) {
                System.out.println("  ! " + e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------ trips

    private void tripMenu() {
        while (true) {
            System.out.println("\n-- Trips --  1.Add 2.List 3.Search 4.Update 5.Delete 0.Back");
            int c = in.integer("Choice: ", 0, 5);
            if (c == 0) {
                return;
            }
            try {
                switch (c) {
                    case 1:
                        Trip t = ctx.tripService.add(in.requiredText("Flight no: "), in.requiredText("Origin: "),
                                in.requiredText("Destination: "), in.dateTime("Departure"),
                                in.integer("Total seats: ", 1, 500), in.decimal("Base fare (Rs): "));
                        System.out.println("  Added trip #" + t.getId());
                        break;
                    case 2:
                        printList(ctx.tripService.list());
                        break;
                    case 3:
                        printList(ctx.tripService.search(in.text("Origin (blank = any): "),
                                in.text("Destination (blank = any): ")));
                        break;
                    case 4:
                        int id = in.integer("Trip id: ");
                        System.out.println("  Current: " + ctx.tripService.get(id));
                        ctx.tripService.update(id, in.dateTime("New departure"), in.integer("New total seats: ", 1, 500),
                                in.decimal("New base fare: "));
                        System.out.println("  Updated.");
                        break;
                    case 5:
                        ctx.tripService.delete(in.integer("Trip id: "));
                        System.out.println("  Deleted.");
                        break;
                    default:
                        break;
                }
            } catch (TicketingException e) {
                System.out.println("  ! " + e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------ bookings

    private void bookingMenu() {
        while (true) {
            System.out.println("\n-- Bookings --  1.Book seat+baggage 2.List all 3.View one 4.By passenger"
                    + " 5.Cancel 6.Free seats 0.Back");
            int c = in.integer("Choice: ", 0, 6);
            if (c == 0) {
                return;
            }
            try {
                switch (c) {
                    case 1:
                        bookSeat();
                        break;
                    case 2:
                        printList(ctx.bookingService.listBookings());
                        break;
                    case 3:
                        showBooking(ctx.bookingService.getBooking(in.integer("Booking id: ")));
                        break;
                    case 4:
                        printList(ctx.bookingService.bookingsOfPassenger(in.integer("Passenger id: ")));
                        break;
                    case 5:
                        ctx.bookingService.cancelBooking(in.integer("Booking id: "));
                        System.out.println("  Booking cancelled; seat released and baggage cancelled.");
                        break;
                    case 6:
                        showFreeSeats(in.integer("Trip id: "));
                        break;
                    default:
                        break;
                }
            } catch (TicketingException e) {
                System.out.println("  ! " + e.getMessage());
            }
        }
    }

    private void bookSeat() {
        printList(ctx.passengerService.list());
        int passengerId = in.integer("Passenger id: ");
        ctx.passengerService.get(passengerId);
        printList(ctx.tripService.list());
        int tripId = in.integer("Trip id: ");
        showFreeSeats(tripId);
        int seat = in.integer("Seat number: ");
        TravelClass cls = in.travelClass();
        System.out.println("  Allowance per bag: " + cls.getFreeAllowanceKg() + " kg free, max " + cls.getMaxBags()
                + " bags, Rs " + cls.getExtraRatePerKg() + " per extra kg.");
        List<Double> weights = in.weights("Bag weights in kg separated by spaces (blank = no bags): ");

        double[] quote = ctx.bookingService.quote(tripId, cls, weights);
        System.out.printf("  Fare Rs %.2f + baggage fee Rs %.2f = TOTAL Rs %.2f%n", quote[0], quote[1], quote[0] + quote[1]);
        if (!in.yesNo("Confirm booking?")) {
            System.out.println("  Booking abandoned.");
            return;
        }
        Booking b = ctx.bookingService.bookSeat(passengerId, tripId, seat, cls, weights);
        System.out.println("  Booked! Booking id #" + b.getId());
        ctx.bookingService.baggageOf(b.getId()).forEach(g -> System.out.println("   bag " + g));
        if (in.yesNo("Export ticket to a file?")) {
            exportTicket(b.getId());
        }
    }

    private void showFreeSeats(int tripId) {
        Trip t = ctx.tripService.get(tripId);
        List<Integer> free = ctx.bookingService.availableSeats(tripId);
        System.out.println("  " + t.getFlightNo() + ": " + free.size() + " of " + t.getTotalSeats() + " seats free");
        System.out.println("  Free: " + free.stream().map(String::valueOf).collect(Collectors.joining(" ")));
    }

    private void showBooking(Booking b) {
        System.out.println("  " + b);
        System.out.println("  Passenger: " + ctx.passengerService.get(b.getPassengerId()));
        System.out.println("  Trip     : " + ctx.tripService.get(b.getTripId()));
        List<Baggage> bags = ctx.bookingService.baggageOf(b.getId());
        System.out.println("  Baggage  : " + (bags.isEmpty() ? "none" : ""));
        bags.forEach(g -> System.out.println("     " + g));
    }

    // ------------------------------------------------------------------ baggage

    private void baggageMenu() {
        BaggageProcessor proc = ctx.baggageProcessor;
        while (true) {
            System.out.println("\n-- Baggage --  1.All bags 2.Bags of a booking 3.Queue status 4.Wait for loader 0.Back");
            int c = in.integer("Choice: ", 0, 4);
            if (c == 0) return;
            try {
                switch (c) {
                    case 1:
                        printList(ctx.bookingService.allBaggage());
                        break;
                    case 2:
                        printList(ctx.bookingService.baggageOf(in.integer("Booking id: ")));
                        break;
                    case 3:
                        System.out.println("  Waiting in queue: " + proc.queueSize() + ", not yet loaded: " + proc.pendingCount()
                                + ", loaded so far: " + proc.getLoadedCount());
                        break;
                    case 4:
                        System.out.println("  Waiting for the loader thread to finish ...");
                        proc.awaitIdle();
                        System.out.println("  All checked-in baggage has been loaded.");
                        break;
                    default:
                        break;
                }
            } catch (ticketing.exception.TicketingException e) {
                System.out.println("  ! " + e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------ reports

    private void reportMenu() {
        while (true) {
            System.out.println("\n-- Reports --  1.Revenue 2.Occupancy 3.Baggage fees 4.Top passengers"
                    + " 5.Heaviest bags 6.Destinations 7.All 0.Back");
            int c = in.integer("Choice: ", 0, 7);
            if (c == 0) return;
            ctx.baggageProcessor.awaitIdle();       // make sure statuses are up to date
            System.out.println();
            if (c == 1 || c == 7) {
                System.out.println(ctx.reportService.revenueReport());
            }
            if (c == 2 || c == 7) {
                System.out.println(ctx.reportService.occupancyReport());
            }
            if (c == 3 || c == 7) {
                System.out.println(ctx.reportService.baggageFeeReport());
            }
            if (c == 4 || c == 7) {
                System.out.println(ctx.reportService.topPassengers(0, 5));
            }
            if (c == 5 || c == 7) {
                System.out.println(ctx.reportService.heaviestBags(5));
            }
            if (c == 6 || c == 7) {
                System.out.println(ctx.reportService.destinations());
            }
        }
    }

    // ------------------------------------------------------------------ files

    private void fileMenu() {
        while (true) {
            System.out.println("\n-- Files --  1.Import passengers CSV 2.Import trips CSV 3.Export ticket"
                    + " 4.Export bookings CSV 5.Backup bookings CSV (byte copy) 0.Back");
            int c = in.integer("Choice: ", 0, 5);
            if (c == 0) return;
            try {
                switch (c) {
                    case 1:
                        System.out.println(CsvImporter.importPassengers(pathOrDefault("data/passengers.csv"),
                                ctx.passengerService));
                        break;
                    case 2:
                        System.out.println(CsvImporter.importTrips(pathOrDefault("data/trips.csv"), ctx.tripService));
                        break;
                    case 3:
                        exportTicket(in.integer("Booking id: "));
                        break;
                    case 4:
                        String out = TicketExporter.exportBookingsCsv(ctx.bookingService.listBookings(),
                                "exports/bookings_export.csv");
                        System.out.println("  Written to " + out);
                        break;
                    case 5:
                        String src = TicketExporter.exportBookingsCsv(ctx.bookingService.listBookings(),
                                "exports/bookings_export.csv");
                        String dst = "exports/backup/bookings_" + System.currentTimeMillis() + ".csv";
                        System.out.println("  Copied " + FileBackup.copy(src, dst) + " bytes to " + dst);
                        break;
                    default:
                        break;
                }
            } catch (ticketing.exception.TicketingException e) {
                System.out.println("  ! " + e.getMessage());
            }
        }
    }

    private String pathOrDefault(String def) {
        String s = in.text("File path (Enter = " + def + "): ");
        return s.isEmpty() ? def : s;
    }

    private void exportTicket(int bookingId) {
        Booking b = ctx.bookingService.getBooking(bookingId);
        String file = TicketExporter.exportTicket(b, ctx.passengerService.get(b.getPassengerId()),
                ctx.tripService.get(b.getTripId()), ctx.bookingService.baggageOf(bookingId), "tickets");
        System.out.println("  Ticket written to " + file);
    }

    // ------------------------------------------------------------------ concurrency

    private void simulation() {
        int seats = in.integer("Seats on the test flight (1-100): ", 1, 100);
        int threads = in.integer("Competing passenger threads (1-500): ", 1, 500);
        try {
            System.out.println(ConcurrencySimulation.run(seats, threads));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ------------------------------------------------------------------ helpers

    private <T> void printList(List<T> items) {
        if (items.isEmpty()) {
            System.out.println("  (nothing found)");
        } else {
            items.forEach(i -> System.out.println("  " + i));
        }
    }
}
