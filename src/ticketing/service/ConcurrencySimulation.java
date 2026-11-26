package ticketing.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ticketing.concurrent.BaggageProcessor;
import ticketing.concurrent.StartGate;
import ticketing.model.Booking;
import ticketing.model.Passenger;
import ticketing.model.TravelClass;
import ticketing.model.Trip;
import ticketing.repository.InMemoryBookingRepository;
import ticketing.repository.InMemoryRepository;

/**
 * Many passenger threads fight for the same few seats at the same instant.
 * Expected result: confirmed bookings == number of seats, and every seat number appears exactly once.
 * Runs on its own private in-memory repositories so it never touches real data.
 */
public final class ConcurrencySimulation {
    private ConcurrencySimulation() {
    }

    public static String run(int seats, int threadCount) throws InterruptedException {
        InMemoryRepository<Passenger> passengerRepo = new InMemoryRepository<>();
        InMemoryRepository<Trip> tripRepo = new InMemoryRepository<>();
        InMemoryBookingRepository bookingRepo = new InMemoryBookingRepository();
        BaggageProcessor processor = new BaggageProcessor(bookingRepo, 5, 2, false);  // small queue => producers block
        BookingService service = new BookingService(passengerRepo, tripRepo, bookingRepo, processor);

        Trip trip = tripRepo.save(new Trip("SIM101", "Delhi", "Mumbai",
                LocalDateTime.now().plusDays(7), seats, 5000));

        StartGate gate = new StartGate();
        AtomicInteger success = new AtomicInteger();
        AtomicInteger soldOut = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();

        for (int i = 1; i <= threadCount; i++) {
            Passenger p = passengerRepo.save(new Passenger("Sim Passenger " + i,
                    "sim" + i + "@mail.com", "9000000000", String.format("SIM%05d", i)));
            Thread t = new Thread(() -> {
                try {
                    gate.await();                                    // everybody starts together
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                List<Integer> order = IntStream.rangeClosed(1, seats).boxed().collect(Collectors.toList());
                Collections.shuffle(order);                          // each thread tries seats in random order
                double weight = 15 + ThreadLocalRandom.current().nextInt(0, 13);   // 15..27 kg
                for (int seat : order) {
                    try {
                        service.bookSeat(p.getId(), trip.getId(), seat, TravelClass.ECONOMY,
                                Arrays.asList(weight));
                        success.incrementAndGet();
                        return;
                    } catch (ticketing.exception.SeatUnavailableException e) {
                        // somebody else got it first: try the next seat
                    }
                }
                soldOut.incrementAndGet();
            }, "passenger-" + i);
            threads.add(t);
            t.start();
        }

        long start = System.nanoTime();
        gate.open();
        for (Thread t : threads) {
            t.join();
        }
        long ms = (System.nanoTime() - start) / 1_000_000;
        processor.awaitIdle();

        List<Booking> confirmed = bookingRepo.findAll().stream().filter(Booking::isConfirmed)
                .collect(Collectors.toList());
        long distinctSeats = confirmed.stream().map(Booking::getSeatNo).distinct().count();
        boolean pass = confirmed.size() == seats && distinctSeats == seats && success.get() == seats;
        int loaded = processor.getLoadedCount();
        processor.shutdown();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("  Seats available            : %d%n", seats));
        sb.append(String.format("  Competing passenger threads: %d%n", threadCount));
        sb.append(String.format("  Successful bookings        : %d%n", success.get()));
        sb.append(String.format("  Turned away (sold out)     : %d%n", soldOut.get()));
        sb.append(String.format("  Distinct seats in database : %d%n", distinctSeats));
        sb.append(String.format("  Bags loaded by consumer    : %d%n", loaded));
        sb.append(String.format("  Time                       : %d ms%n", ms));
        sb.append(pass ? "  RESULT: PASS - no seat was double-booked" : "  RESULT: FAIL - double booking detected!");
        return sb.toString();
    }
}
