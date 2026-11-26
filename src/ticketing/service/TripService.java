package ticketing.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import ticketing.exception.NotFoundException;
import ticketing.exception.PersistenceException;
import ticketing.exception.ValidationException;
import ticketing.model.Booking;
import ticketing.model.Trip;
import ticketing.repository.BookingRepository;
import ticketing.repository.Repository;
import ticketing.util.Validator;

public class TripService {
    public static final int MAX_SEATS = 500;

    private final Repository<Trip> repo;
    private final BookingRepository bookings;

    public TripService(Repository<Trip> repo, BookingRepository bookings) {
        this.repo = repo;
        this.bookings = bookings;
    }

    public synchronized Trip add(String flightNo, String origin, String destination,
                                 LocalDateTime departure, int seats, double baseFare) {
        String fn = Validator.requireNonBlank(flightNo, "Flight number").toUpperCase();
        String from = Validator.requireNonBlank(origin, "Origin");
        String to = Validator.requireNonBlank(destination, "Destination");
        if (from.equalsIgnoreCase(to)) {
            throw new ValidationException("Origin and destination must be different");
        }
        if (departure == null || departure.isBefore(LocalDateTime.now())) {
            throw new ValidationException("Departure must be in the future");
        }
        Validator.inRange(seats, 1, MAX_SEATS, "Total seats");
        Validator.positive(baseFare, "Base fare");
        boolean duplicate = repo.findAll().stream()
                .anyMatch(t -> t.getFlightNo().equals(fn) && t.getDeparture().equals(departure));
        if (duplicate) {
            throw new ValidationException("Flight " + fn + " already exists at " + departure);
        }
        return repo.save(new Trip(fn, from, to, departure, seats, baseFare));
    }

    public Trip get(int id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("No trip with id " + id));
    }

    public List<Trip> list() {
        return repo.findAll();
    }

    /** Blank filter = match everything. */
    public List<Trip> search(String origin, String destination) {
        String o = origin.trim().toLowerCase();
        String d = destination.trim().toLowerCase();
        return repo.findAll().stream()
                .filter(t -> t.getOrigin().toLowerCase().contains(o))
                .filter(t -> t.getDestination().toLowerCase().contains(d))
                .collect(Collectors.toList());
    }

    public synchronized Trip update(int id, LocalDateTime departure, int seats, double baseFare) {
        Trip t = get(id);
        if (departure.isBefore(LocalDateTime.now())) {
            throw new ValidationException("Departure must be in the future");
        }
        Validator.inRange(seats, 1, MAX_SEATS, "Total seats");
        Validator.positive(baseFare, "Base fare");
        int highestSeat = bookings.findAll().stream()
                .filter(b -> b.getTripId() == id && b.isConfirmed())
                .mapToInt(Booking::getSeatNo)
                .max().orElse(0);
        if (seats < highestSeat) {
            throw new ValidationException("Seat " + highestSeat + " is already booked; seats cannot go below that");
        }
        t.setDeparture(departure);
        t.setTotalSeats(seats);
        t.setBaseFare(baseFare);
        if (!repo.update(t)) {
            throw new PersistenceException("Update failed for trip " + id, null);
        }
        return t;
    }

    public synchronized void delete(int id) {
        get(id);
        boolean hasBookings = bookings.findAll().stream().anyMatch(b -> b.getTripId() == id);
        if (hasBookings) {
            throw new ValidationException("Trip " + id + " has bookings and cannot be deleted");
        }
        repo.delete(id);
    }
}
