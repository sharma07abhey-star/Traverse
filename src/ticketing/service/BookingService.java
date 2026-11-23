package ticketing.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ticketing.concurrent.BaggageProcessor;
import ticketing.exception.NotFoundException;
import ticketing.exception.SeatUnavailableException;
import ticketing.exception.ValidationException;
import ticketing.model.Baggage;
import ticketing.model.Booking;
import ticketing.model.Passenger;
import ticketing.model.Trip;
import ticketing.model.TravelClass;
import ticketing.repository.BookingRepository;
import ticketing.repository.Repository;

/**
 * Core workflow: reserve a seat AND register baggage in one step.
 *
 * Double-booking protection (3 layers):
 *   1. a per-trip lock object -> the "is the seat free? then book it" sequence is atomic per trip
 *   2. the repository itself re-checks inside its own synchronized / transactional method
 *   3. (JDBC) a UNIQUE index on (trip_id, seat_no) for CONFIRMED bookings
 */
public class BookingService {
    private final Repository<Passenger> passengers;
    private final Repository<Trip> trips;
    private final BookingRepository bookings;
    private final BaggageProcessor baggageProcessor;
    private final Map<Integer, Object> tripLocks = new ConcurrentHashMap<>();

    public BookingService(Repository<Passenger> passengers, Repository<Trip> trips,
                          BookingRepository bookings, BaggageProcessor baggageProcessor) {
        this.passengers = passengers;
        this.trips = trips;
        this.bookings = bookings;
        this.baggageProcessor = baggageProcessor;
    }

    private Object lockFor(int tripId) {
        return tripLocks.computeIfAbsent(tripId, k -> new Object());
    }

    /** Returns {fare, baggageFee} without saving anything. */
    public double[] quote(int tripId, TravelClass cls, List<Double> weights) {
        Trip trip = trips.findById(tripId).orElseThrow(() -> new NotFoundException("No trip with id " + tripId));
        BaggageFeeCalculator.validate(cls, weights);
        return new double[]{trip.getBaseFare() * cls.getFareMultiplier(),
                BaggageFeeCalculator.totalFee(cls, weights)};
    }

    public Booking bookSeat(int passengerId, int tripId, int seatNo, TravelClass cls, List<Double> bagWeights) {
        passengers.findById(passengerId)
                .orElseThrow(() -> new NotFoundException("No passenger with id " + passengerId));
        Trip trip = trips.findById(tripId).orElseThrow(() -> new NotFoundException("No trip with id " + tripId));
        if (cls == null) {
            throw new ValidationException("Travel class is required");
        }
        if (trip.getDeparture().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Flight " + trip.getFlightNo() + " has already departed");
        }
        if (seatNo < 1 || seatNo > trip.getTotalSeats()) {
            throw new ValidationException("Seat must be between 1 and " + trip.getTotalSeats());
        }
        BaggageFeeCalculator.validate(cls, bagWeights);

        List<Baggage> bags = new ArrayList<>();
        for (double w : bagWeights) {
            bags.add(new Baggage(w, BaggageFeeCalculator.feeFor(cls, w)));
        }
        double fare = trip.getBaseFare() * cls.getFareMultiplier();
        double bagFee = bags.stream().mapToDouble(Baggage::getFee).sum();

        Booking saved;
        synchronized (lockFor(tripId)) {                       // critical section: check-then-act
            if (bookings.isSeatTaken(tripId, seatNo)) {
                throw new SeatUnavailableException("Seat " + seatNo + " on " + trip.getFlightNo()
                        + " is already booked");
            }
            saved = bookings.saveWithBaggage(new Booking(passengerId, tripId, seatNo, cls, fare, bagFee), bags);
        }
        // Producer side of the baggage queue. Done OUTSIDE the lock because submit() may block.
        for (Baggage b : bags) {
            baggageProcessor.submit(b);
        }
        return saved;
    }

    public void cancelBooking(int bookingId) {
        Booking b = getBooking(bookingId);
        synchronized (lockFor(b.getTripId())) {
            if (!bookings.cancelBooking(bookingId)) {
                throw new ValidationException("Booking " + bookingId + " is already cancelled");
            }
        }
    }

    public Booking getBooking(int id) {
        return bookings.findById(id).orElseThrow(() -> new NotFoundException("No booking with id " + id));
    }

    public List<Booking> listBookings() {
        return bookings.findAll();
    }

    public List<Booking> bookingsOfPassenger(int passengerId) {
        return bookings.findAll().stream()
                .filter(b -> b.getPassengerId() == passengerId)
                .collect(Collectors.toList());
    }

    public List<Baggage> baggageOf(int bookingId) {
        return bookings.findBaggageByBooking(bookingId);
    }

    public List<Baggage> allBaggage() {
        return bookings.findAllBaggage();
    }

    public List<Integer> availableSeats(int tripId) {
        Trip trip = trips.findById(tripId).orElseThrow(() -> new NotFoundException("No trip with id " + tripId));
        List<Integer> taken = bookings.findAll().stream()
                .filter(b -> b.getTripId() == tripId && b.isConfirmed())
                .map(Booking::getSeatNo)
                .collect(Collectors.toList());
        return IntStream.rangeClosed(1, trip.getTotalSeats())
                .filter(s -> !taken.contains(s))
                .boxed()
                .collect(Collectors.toList());
    }
}
