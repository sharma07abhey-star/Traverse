package ticketing.service;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import ticketing.model.Baggage;
import ticketing.model.BaggageStatus;
import ticketing.model.Booking;
import ticketing.model.Passenger;
import ticketing.model.TravelClass;
import ticketing.model.Trip;
import ticketing.repository.BookingRepository;
import ticketing.repository.Repository;

/** Reports built purely with lambdas and the Stream API (filter, map, sorted, distinct, limit, skip, collect). */
public class ReportService {
    private final Repository<Passenger> passengers;
    private final Repository<Trip> trips;
    private final BookingRepository bookings;

    public ReportService(Repository<Passenger> passengers, Repository<Trip> trips, BookingRepository bookings) {
        this.passengers = passengers;
        this.trips = trips;
        this.bookings = bookings;
    }

    private List<Booking> confirmed() {
        return bookings.findAll().stream().filter(Booking::isConfirmed).collect(Collectors.toList());
    }

    /** Revenue (fare + baggage fee) per trip, highest first. */
    public String revenueReport() {
        Map<Integer, Trip> tripById = trips.findAll().stream().collect(Collectors.toMap(Trip::getId, t -> t));
        Map<Integer, Double> revenue = confirmed().stream()
                .collect(Collectors.groupingBy(Booking::getTripId, Collectors.summingDouble(Booking::getTotal)));

        StringBuilder sb = new StringBuilder("--- Revenue per trip (confirmed bookings) ---\n");
        revenue.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .forEach(e -> sb.append(String.format("  %-8s %-12s -> %-12s Rs %,12.2f%n",
                        tripById.get(e.getKey()).getFlightNo(),
                        tripById.get(e.getKey()).getOrigin(),
                        tripById.get(e.getKey()).getDestination(), e.getValue())));
        double total = revenue.values().stream().mapToDouble(Double::doubleValue).sum();
        sb.append(String.format("  %-36s Rs %,12.2f", "TOTAL", total));
        return sb.toString();
    }

    /** Seats sold vs capacity per trip. */
    public String occupancyReport() {
        Map<Integer, Long> sold = confirmed().stream()
                .collect(Collectors.groupingBy(Booking::getTripId, Collectors.counting()));
        StringBuilder sb = new StringBuilder("--- Seat occupancy ---\n");
        trips.findAll().stream()
                .sorted((a, b) -> Double.compare(occupancy(b, sold), occupancy(a, sold)))
                .forEach(t -> sb.append(String.format("  %-8s %-12s -> %-12s %3d / %-3d seats  %5.1f%%%n",
                        t.getFlightNo(), t.getOrigin(), t.getDestination(),
                        sold.getOrDefault(t.getId(), 0L), t.getTotalSeats(), occupancy(t, sold))));
        return sb.toString().trim();
    }

    private double occupancy(Trip t, Map<Integer, Long> sold) {
        return 100.0 * sold.getOrDefault(t.getId(), 0L) / t.getTotalSeats();
    }

    /** Baggage statistics and fee income split by travel class. */
    public String baggageFeeReport() {
        Map<Integer, Booking> bookingById = confirmed().stream()
                .collect(Collectors.toMap(Booking::getId, Function.identity()));
        List<Baggage> bags = bookings.findAllBaggage().stream()
                .filter(g -> g.getStatus() != BaggageStatus.CANCELLED)
                .collect(Collectors.toList());

        double totalFees = bags.stream().mapToDouble(Baggage::getFee).sum();
        long overweight = bags.stream().filter(g -> g.getFee() > 0).count();
        double avgWeight = bags.stream().mapToDouble(Baggage::getWeightKg).average().orElse(0);

        Map<TravelClass, Double> byClass = bags.stream()
                .filter(g -> bookingById.containsKey(g.getBookingId()))
                .collect(Collectors.groupingBy(g -> bookingById.get(g.getBookingId()).getTravelClass(),
                        () -> new TreeMap<>(), Collectors.summingDouble(Baggage::getFee)));

        StringBuilder sb = new StringBuilder("--- Baggage fees ---\n");
        sb.append(String.format("  Bags checked in (active) : %d%n", bags.size()));
        sb.append(String.format("  Overweight bags          : %d%n", overweight));
        sb.append(String.format("  Average bag weight       : %.1f kg%n", avgWeight));
        sb.append(String.format("  Total baggage fees       : Rs %,.2f%n", totalFees));
        byClass.forEach((cls, fee) -> sb.append(String.format("    %-9s Rs %,.2f%n", cls, fee)));
        return sb.toString().trim();
    }

    /** Top spenders; skip/limit allow simple "paging" (page 0 = first N). */
    public String topPassengers(int skip, int limit) {
        Map<Integer, Passenger> byId = passengers.findAll().stream()
                .collect(Collectors.toMap(Passenger::getId, p -> p));
        Map<Integer, Double> spend = confirmed().stream()
                .collect(Collectors.groupingBy(Booking::getPassengerId, Collectors.summingDouble(Booking::getTotal)));
        StringBuilder sb = new StringBuilder("--- Top passengers by spend ---\n");
        spend.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .skip(skip)
                .limit(limit)
                .forEach(e -> sb.append(String.format("  %-22s Rs %,12.2f%n",
                        byId.get(e.getKey()).getName(), e.getValue())));
        return sb.toString().trim();
    }

    public String heaviestBags(int n) {
        StringBuilder sb = new StringBuilder("--- Heaviest bags ---\n");
        bookings.findAllBaggage().stream()
                .filter(g -> g.getStatus() != BaggageStatus.CANCELLED)
                .sorted((a, b) -> Double.compare(b.getWeightKg(), a.getWeightKg()))
                .limit(n)
                .forEach(g -> sb.append("  ").append(g).append('\n'));
        return sb.toString().trim();
    }

    public String destinations() {
        String list = trips.findAll().stream()
                .map(Trip::getDestination)
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
        return "--- Destinations served ---\n  " + list;
    }
}
