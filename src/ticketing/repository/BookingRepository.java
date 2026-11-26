package ticketing.repository;

import java.util.List;

import ticketing.model.Baggage;
import ticketing.model.Booking;

/** Booking + baggage must always be written together, so they share one repository. */
public interface BookingRepository extends Repository<Booking> {

    /**
     * Atomically stores the booking and all its baggage (JDBC: one transaction, commit or rollback).
     * Sets ids and tag numbers on the objects passed in.
     *
     * @throws ticketing.exception.SeatUnavailableException if the seat is already confirmed for the trip
     */
    Booking saveWithBaggage(Booking booking, List<Baggage> baggage);

    boolean isSeatTaken(int tripId, int seatNo);

    /** Marks the booking and all its baggage as CANCELLED in one atomic step. */
    boolean cancelBooking(int bookingId);

    List<Baggage> findBaggageByBooking(int bookingId);

    List<Baggage> findAllBaggage();

    /** CHECKED_IN -> LOADED. Returns false if the bag was cancelled (or already loaded) meanwhile. */
    boolean markLoaded(int baggageId);
}
