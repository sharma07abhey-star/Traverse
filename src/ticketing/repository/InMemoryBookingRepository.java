package ticketing.repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import ticketing.exception.PersistenceException;
import ticketing.exception.SeatUnavailableException;
import ticketing.model.Baggage;
import ticketing.model.BaggageStatus;
import ticketing.model.Booking;
import ticketing.model.BookingStatus;
import ticketing.util.TagGenerator;

public class InMemoryBookingRepository extends InMemoryRepository<Booking> implements BookingRepository {
    private final Map<Integer, Baggage> bags = new ConcurrentHashMap<>();
    private final AtomicInteger bagSequence = new AtomicInteger();

    /** synchronized => the "check seat, then insert" sequence cannot be interleaved by other threads. */
    @Override
    public synchronized Booking saveWithBaggage(Booking booking, List<Baggage> baggage) {
        if (isSeatTaken(booking.getTripId(), booking.getSeatNo())) {
            throw new SeatUnavailableException("Seat " + booking.getSeatNo() + " is already booked");
        }
        List<Integer> addedBagIds = new ArrayList<>();
        try {
            save(booking);
            int n = 1;
            for (Baggage b : baggage) {
                b.setBookingId(booking.getId());
                b.setTagNo(TagGenerator.tag(booking.getId(), n++));
                b.setId(bagSequence.incrementAndGet());
                bags.put(b.getId(), b);
                addedBagIds.add(b.getId());
            }
            return booking;
        } catch (RuntimeException e) {          // "rollback"
            store.remove(booking.getId());
            addedBagIds.forEach(bags::remove);
            throw new PersistenceException("Booking failed and was rolled back", e);
        }
    }

    @Override
    public boolean isSeatTaken(int tripId, int seatNo) {
        return store.values().stream().anyMatch(b ->
                b.getTripId() == tripId && b.getSeatNo() == seatNo && b.getStatus() == BookingStatus.CONFIRMED);
    }

    @Override
    public synchronized boolean cancelBooking(int bookingId) {
        Booking b = store.get(bookingId);
        if (b == null || b.getStatus() != BookingStatus.CONFIRMED) {
            return false;
        }
        b.setStatus(BookingStatus.CANCELLED);
        bags.values().stream()
                .filter(g -> g.getBookingId() == bookingId)
                .forEach(g -> g.setStatus(BaggageStatus.CANCELLED));
        return true;
    }

    @Override
    public List<Baggage> findBaggageByBooking(int bookingId) {
        return bags.values().stream()
                .filter(g -> g.getBookingId() == bookingId)
                .sorted(Comparator.comparingInt(Baggage::getId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Baggage> findAllBaggage() {
        return bags.values().stream()
                .sorted(Comparator.comparingInt(Baggage::getId))
                .collect(Collectors.toList());
    }

    @Override
    public synchronized boolean markLoaded(int baggageId) {
        Baggage g = bags.get(baggageId);
        if (g == null || g.getStatus() != BaggageStatus.CHECKED_IN) {
            return false;
        }
        g.setStatus(BaggageStatus.LOADED);
        return true;
    }
}
